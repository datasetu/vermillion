package vermillion.http;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Optional;

public class HttpServerVerticle extends AbstractVerticle {
    public static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    // HTTP Codes
    public final int OK = 200;

    // Auth server constants
    public final String AUTH_SERVER = System.getenv("AUTH_SERVER");
    public final String INTROSPECT_ENDPOINT = "/auth/v1/token/introspect";
    public final String AUTH_TLS_CERT_PATH = System.getenv("AUTH_TLS_CERT_PATH");
    public final String AUTH_TLS_CERT_PASSWORD = System.getenv("AUTH_TLS_CERT_PASSWORD");

    // Redis constants
    public final String REDIS_HOST = System.getenv("REDIS_HOSTNAME");

    /* Default port of redis. Port specified in the config file will
    	 not affect the default port to which redis is going to bind to
    */
    public final String REDIS_PORT = "6379";
    public final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");

    // There are 16 DBs available. Using 1 as the default database number
    public final String DB_NUMBER = "1";
    public final String CONNECTION_STR =
            "redis://:" + REDIS_PASSWORD + "@" + REDIS_HOST + ":" + REDIS_PORT + "/" + DB_NUMBER;
    public final int MAX_POOL_SIZE = 10;
    public final int MAX_WAITING_HANDLERS = 32;

    public RedisOptions options;

    @Override
    public void start(Promise<Void> promise) {
        logger.debug("In start");

        int port = 80;

        Router router = Router.router(vertx);

        router.get("/auth/user").handler(this::authUser);
        router.get("/auth/vhost").handler(this::authVhost);
        router.get("/auth/topic").handler(this::authTopic);
        router.get("/auth/resource").handler(this::authResource);

        options = new RedisOptions()
                .setConnectionString(CONNECTION_STR)
                .setMaxPoolSize(MAX_POOL_SIZE)
                .setMaxWaitingHandlers(MAX_WAITING_HANDLERS);

        vertx.createHttpServer()
                .requestHandler(router)
                .rxListen(port)
                .subscribe(
                        s -> {
                            logger.debug("Server started");
                            promise.complete();
                        },
                        err -> {
                            logger.debug("Could not start server. Cause=" + err.getMessage());
                            promise.fail(err.getMessage());
                        });
    }

    public Single<RedisAPI> getRedisCient() {
        return Redis.createClient(vertx, options).rxConnect().map(RedisAPI::api);
    }

    public Single<String> getValue(String key) {

        return getRedisCient()
                .flatMapMaybe(redisAPI -> redisAPI.rxGet(key))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .map(value -> value.isPresent() ? value.get().toString() : "absent");
    }

    public Completable setValue(String key, String value) {
        ArrayList<String> list = new ArrayList<>();

        list.add(key);
        list.add(value);

        return getRedisCient().flatMapCompletable(redisAPI -> Completable.fromMaybe(redisAPI.rxSet(list)));
    }

    public void authUser(RoutingContext context) {

        logger.debug("In auth user");

        HttpServerRequest request = context.request();
        HttpServerResponse resp = request.response();

        // Ignore the password parameter. Username is the token
        String username = URLDecoder.decode(request.getParam("username"), StandardCharsets.UTF_8);

        logger.debug("Token=" + username);

        if (!isStringSafe(username)) {
            logger.debug("invalid entity name");
            ok(resp, "deny");
            return;
        }

        getValue(username)
                .flatMapCompletable(
                        cache -> cache.equalsIgnoreCase("absent") ? introspect(username) : Completable.complete())
                .subscribe(() -> ok(resp, "allow"), t -> apiFailure(context, t));
    }

    public void authVhost(RoutingContext context) {

        logger.debug("In auth vhost");
        HttpServerResponse resp = context.response();
        ok(resp, "allow");
    }

    public void authTopic(RoutingContext context) {

        logger.debug("In auth topic");
        HttpServerResponse resp = context.response();
        HttpServerRequest request = context.request();

        String username = request.getParam("username");
        String resourceType = request.getParam("resource");
        String resourceName = request.getParam("name");
        String permission = request.getParam("permission");
        String routingKey = request.getParam("routing_key");

        logger.debug("Username=" + username);
        logger.debug("ResourceType=" + resourceType);
        logger.debug("ResourceName=" + resourceName);
        logger.debug("Permission=" + permission);
        logger.debug("RoutingKey=" + routingKey);

        if ("configure".equalsIgnoreCase(permission) || "read".equalsIgnoreCase(permission)) {

            logger.debug("Denied due to requested permission");
            ok(resp, "deny");
            return;
        }

        if (!"exchange".equalsIgnoreCase(resourceType) && !"topic".equalsIgnoreCase(resourceType)) {

            logger.debug("Denied since resource is not an exchange");
            ok(resp, "deny");
            return;
        }

        if (!resourceName.equalsIgnoreCase("EXCHANGE")) {

            logger.debug("Denied since resource name is not EXCHANGE");
            ok(resp, "deny");
            return;
        }

        getValue(username)
                .flatMapCompletable(result -> {
                    JsonArray authorisedRequests = new JsonObject(result).getJsonArray("request");
                    for (int i = 0; i < authorisedRequests.size(); i++) {
                        JsonObject requestObject = authorisedRequests.getJsonObject(i);
                        logger.debug("Authorised request=" + requestObject.toString());

                        /* Return if a match is found. Cannot request for multiple IDs
                         * in a single request
                         */

                        if (routingKey.equalsIgnoreCase(requestObject.getString("id"))) {
                            return Completable.complete();
                        }
                    }
                    return Completable.error(new Throwable("Unauthorised"));
                })
                .subscribe(() -> ok(resp, "allow"), t -> apiFailure(context, t));
    }

    public void authResource(RoutingContext context) {

        logger.debug("In auth resource");
        HttpServerRequest request = context.request();
        HttpServerResponse resp = request.response();
        String username = request.getParam("username");
        String resourceType = request.getParam("resource");
        String resourceName = request.getParam("name");
        String permission = request.getParam("permission");

        logger.debug("Username=" + username);
        logger.debug("ResourceType=" + resourceType);
        logger.debug("ResourceName=" + resourceName);
        logger.debug("Permission=" + permission);

        if ("configure".equalsIgnoreCase(permission) || "read".equalsIgnoreCase(permission)) {

            logger.debug("Denied due to requested permission");
            ok(resp, "deny");
            return;
        }

        if (!"exchange".equals(resourceType) && !"topic".equalsIgnoreCase(resourceType)) {

            logger.debug("Denied since requested resource type is not an exchange");
            ok(resp, "deny");
            return;
        }

        if (!resourceName.equalsIgnoreCase("EXCHANGE")) {

            logger.debug("Denied since resource name is not EXCHANGE");
            ok(resp, "deny");
            return;
        }

        logger.debug("Allowed");
        ok(resp, "allow");
    }

    public boolean isStringSafe(String resource) {
        logger.debug("In is_string_safe");

        logger.debug("resource=" + resource);

        boolean safe = (resource.length() - (resource.replaceAll("[^a-zA-Z0-9-_./@]+", "")).length()) == 0;

        logger.debug("Original resource name =" + resource);
        logger.debug("Replaced resource name =" + resource.replaceAll("[^a-zA-Z0-9-_./@]+", ""));
        return safe;
    }

    public void ok(HttpServerResponse resp, String message) {
        if (!resp.closed()) {
            resp.setStatusCode(OK).end(message);
        }
    }

    private Completable introspect(String token) {

        JsonObject body = new JsonObject();
        body.put("token", token);

        WebClientOptions options = new WebClientOptions()
                .setSsl(true)
                .setKeyStoreOptions(new JksOptions().setPath(AUTH_TLS_CERT_PATH).setPassword(AUTH_TLS_CERT_PASSWORD));

        WebClient client = WebClient.create(vertx, options);

        return client.post(443, AUTH_SERVER, INTROSPECT_ENDPOINT)
                .ssl(true)
                .putHeader("content-type", "application/json")
                .rxSendJsonObject(body)
                .flatMapMaybe(response -> {
                    if (response.statusCode() == 200) return Maybe.just(response.bodyAsString());
                    else {
                        logger.debug(response.bodyAsString());
                        return Maybe.empty();
                    }
                })
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapCompletable(data -> (data.isPresent())
                        ? setValue(token, data.get())
                        : Completable.error(new Throwable("Unauthorised")));
    }

    public void apiFailure(RoutingContext context, Throwable t) {
        logger.debug("In apifailure");
        logger.debug("Message=" + t.getMessage());
        context.response().setStatusCode(OK).end("deny");
    }
}
