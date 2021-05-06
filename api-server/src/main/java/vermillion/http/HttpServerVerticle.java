package vermillion.http;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.FileUpload;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import vermillion.database.reactivex.DbService;
import vermillion.broker.reactivex.BrokerService;
import vermillion.http.Latest;
import vermillion.http.Search;
import vermillion.http.Download;
import vermillion.http.Publish;
import vermillion.http.Unpublish;
import vermillion.http.ScrolledSearch;
import vermillion.throwables.UnauthorisedThrowable;

import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HttpServerVerticle extends AbstractVerticle {

    public final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);


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
    public final String CONNECTION_STR = "redis://:" + REDIS_PASSWORD + "@" + REDIS_HOST + "/" + DB_NUMBER;

    // public final String CONNECTION_STR =
    //				"redis://:" + REDIS_PASSWORD + "@" + REDIS_HOST + ":" + REDIS_PORT + "/" + DB_NUMBER;
    public final int MAX_POOL_SIZE = 10;
    public final int MAX_WAITING_HANDLERS = 32;

    // Certificate constants
    public final String SSL_CERT_NAME = System.getenv("SSL_CERT_NAME");
    public final String SSL_CERT_PASSWORD = System.getenv("SSL_CERT_PASSWORD");

    // HTTPS port
    public final int HTTPS_PORT = 443;
    // RabbitMQ default exchange to publish to
    public final String RABBITMQ_PUBLISH_EXCHANGE = "EXCHANGE";
    public String CONSUMER_PATH = "/consumer/";
    // Service Proxies
    public static DbService dbService;
    public static BrokerService brokerService;
    public static RedisOptions options;
    public static io.vertx.reactivex.core.Vertx vert;

    @Override
    public void start(Promise<Void> startPromise) {
        logger.debug("In start");
        logger.debug("auth server=" + AUTH_SERVER);
        logger.debug("Redis constr=" + CONNECTION_STR);

        vert = vertx;
        dbService = vermillion.database.DbService.createProxy(vert.getDelegate(), "db.queue");
        brokerService = vermillion.broker.BrokerService.createProxy(vert.getDelegate(), "broker.queue");
        Router router = Router.router(vert);

        router.route().handler(BodyHandler.create().setHandleFileUploads(true));

        // Serve API docs at /
        router.route("/").handler(StaticHandler.create("webroot"));

        router.get("/latest").handler(new Latest()::latest);
        router.post("/search").handler(new Search()::search);

        // The path described by the regex is /consumer/<auth_server>/<token>/*
        router.routeWithRegex("\\/consumer\\/" + AUTH_SERVER + "\\/[0-9a-f]+\\/?.*")
                .handler(StaticHandler.create()
                        .setAllowRootFileSystemAccess(false)
                        .setDirectoryListing(true));

        // The path described by the regex is /provider/public/<domain>/<sha1>/<rs_name>/*
        router.routeWithRegex("\\/provider\\/public\\/?.*")
                .handler(StaticHandler.create()
                        .setAllowRootFileSystemAccess(false)
                        .setDirectoryListing(true));

        router.get("/download").handler(new Download()::download);
        router.post("/publish/file").handler(new Publish()::publishFile);
        router.post("/publish/data").handler(new Publish()::publishData);
        router.post("/unpublish").handler(new Unpublish()::unpublish);
        router.post("/search/scroll").handler(new ScrolledSearch()::scrolledSearch);

        options = new RedisOptions()
                .setConnectionString(CONNECTION_STR)
                .setMaxPoolSize(MAX_POOL_SIZE)
                .setMaxWaitingHandlers(MAX_WAITING_HANDLERS);

        vert.createHttpServer(new HttpServerOptions()
                        .setSsl(true)
                        .setCompressionSupported(true)
                        .setKeyStoreOptions(
                                new JksOptions().setPath(SSL_CERT_NAME).setPassword(SSL_CERT_PASSWORD)))
                .requestHandler(router)
                .rxListen(HTTPS_PORT)
                .subscribe(
                        s -> {
                            logger.debug("Server started");
                            startPromise.complete();
                        },
                        err -> {
                            logger.debug("Could not start server. Cause=" + err.getMessage());
                            startPromise.fail(err.getMessage());
                        });
    }

    // TODO: Check why using a custom conf file fails here
    public Single<RedisAPI> getRedisClient() {
        logger.debug("In get redis client");
        logger.debug("options=" + options.toJson().encodePrettily());
        return Redis.createClient(vert, options).rxConnect().map(RedisAPI::api);
    }

    public Single<String> getValue(String key) {

        logger.debug("In getValue");

        // TODO: Initiate introspect here instead of calling the API twice
        return getRedisClient()
                .flatMapMaybe(redisAPI -> {
                    logger.debug("Got redis client");
                    return redisAPI.rxGet(key);
                })
                .map(value -> {
                    logger.debug("Value=" + value.toString());
                    return Optional.of(value);
                })
                .toSingle(Optional.empty())
                .map(value -> value.isPresent() ? value.get().toString() : "absent");
    }

    public Completable setValue(String key, String value) {

        logger.debug("In set value");
        ArrayList<String> list = new ArrayList<>();

        list.add(key);
        list.add(value);

        return getRedisClient().flatMapCompletable(redisAPI -> Completable.fromMaybe(redisAPI.rxSet(list)));
    }

    // TODO: Handle server token
    // TODO: Handle regexes
    // Method that makes the HTTPS request to the auth server
    public Completable introspect(String token) {
        logger.debug("In introspect");
        JsonObject body = new JsonObject();
        body.put("token", token);

        WebClientOptions webClientOptions = new WebClientOptions()
                .setSsl(true)
                .setKeyStoreOptions(new JksOptions().setPath(AUTH_TLS_CERT_PATH).setPassword(AUTH_TLS_CERT_PASSWORD));

        if ("auth.local".equalsIgnoreCase(AUTH_SERVER)) {
            webClientOptions.setTrustAll(true);
        }

        WebClient client = WebClient.create(vert, webClientOptions);

        return client.post(443, AUTH_SERVER, INTROSPECT_ENDPOINT)
                .ssl(true)
                .putHeader("content-type", "application/json")
                .rxSendJsonObject(body)
                .flatMapMaybe(response -> {
                    if (response.statusCode() == 200) {
                        return Maybe.just(response.bodyAsString());
                    } else {
                        logger.debug("Auth response=" + response.bodyAsString());
                        return Maybe.empty();
                    }
                })
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapCompletable(data -> (data.isPresent())
                        ? setValue(token, data.get())
                        : Completable.error(new UnauthorisedThrowable("Unauthorised")));
    }

    // Method that uses the redis cache for authorising requests.
    // Uses introspect if needed
    public Completable checkAuthorisation(String token, String scope, JsonArray requestedIds) {

        Set<String> requestedSet = IntStream.range(0, requestedIds.size())
                .mapToObj(requestedIds::getString)
                .collect(Collectors.toCollection(HashSet::new));

        return getValue(token)
                .flatMapCompletable(
                        cache -> "absent".equalsIgnoreCase(cache) ? introspect(token) : Completable.complete())
                // TODO: Avoid reading from cache again
                .andThen(Single.defer(() -> getValue(token)))
                .flatMapMaybe(result -> "absent".equalsIgnoreCase(result)
                        ? Maybe.empty()
                        : Maybe.just(new JsonObject(result).getJsonArray("request")))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapMaybe(resultArray -> resultArray
                        .map(authorisedIds ->
                                // TODO: In this case check for methods,
                                // body, API etc
                                Maybe.just(IntStream.range(0, authorisedIds.size())
                                        .filter(i -> authorisedIds
                                                .getJsonObject(i)
                                                .getJsonArray("scopes")
                                                .contains(scope))
                                        .mapToObj(i ->
                                                authorisedIds.getJsonObject(i).getString("id"))
                                        .collect(Collectors.toCollection(HashSet::new))))
                        .orElseGet(Maybe::empty))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapCompletable(hashSet -> hashSet.map(authorisedSet -> (authorisedSet.containsAll(requestedSet)
                        ? Completable.complete()
                        : Completable.error(new UnauthorisedThrowable("ACL does not match"))))
                        .orElseGet(() -> Completable.error(new UnauthorisedThrowable("Unauthorised"))));
    }

    public Single<JsonArray> checkAuthorisation(String token, String scope) {

        logger.debug("In check authorisation");

        return getValue(token)
                .flatMapCompletable(
                        cache -> "absent".equalsIgnoreCase(cache) ? introspect(token) : Completable.complete())
                // TODO: Avoid reading from cache again
                .andThen(Single.defer(() -> getValue(token)))
                .flatMapMaybe(result -> "absent".equalsIgnoreCase(result)
                        ? Maybe.empty()
                        : Maybe.just(new JsonObject(result).getJsonArray("request")))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapMaybe(resultArray -> resultArray
                        .map(authorisedIds ->
                                // TODO: In this case check for methods, body, API etc
                                Maybe.just(new JsonArray(IntStream.range(0, authorisedIds.size())
                                        .filter(i -> authorisedIds
                                                .getJsonObject(i)
                                                .getJsonArray("scopes")
                                                .contains(scope))
                                        .mapToObj(i ->
                                                authorisedIds.getJsonObject(i).getString("id"))
                                        .collect(Collectors.toList()))))
                        .orElseGet(Maybe::empty))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMap(result -> result.map(Single::just)
                        .orElseGet(() -> Single.error(new UnauthorisedThrowable("Unauthorised"))));
    }
}
