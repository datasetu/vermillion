package vermillion.http;

import io.reactivex.Completable;
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

import java.util.HashMap;
import java.util.Map;

public class HttpServerVerticle extends AbstractVerticle {
  public static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

  // HTTP Codes
  public final int OK = 200;

  //Use a database for this
  public Map<String, JsonObject> introspectedCache = new HashMap<>();
  public Map<String, JsonObject> usernameCache = new HashMap<>();

  @Override
  public void start(Promise<Void> promise) {
    logger.debug("In start");

    int port = 80;

    Router router = Router.router(vertx);

    router.get("/auth/user").handler(this::authUser);
    router.get("/auth/vhost").handler(this::authVhost);
    router.get("/auth/topic").handler(this::authTopic);
    router.get("/auth/resource").handler(this::authResource);

    vertx
        .createHttpServer()
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

  public void authUser(RoutingContext context) {

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String username = request.getParam("username");
    String password = request.getParam("password");

    if (!(isStringSafe(username)) || !(isStringSafe(password))) {
      logger.debug("invalid entity name");
      ok(resp, "deny");
      return;
    }

    /* Check if supplied username is same as actual username applicable for the token */
    if (introspectedCache.containsKey(password)) {
      logger.debug("Already introspected");
      if (introspectedCache.get(password).getString("consumer").equalsIgnoreCase(username))
        ok(resp, "allow");
      else {
        logger.debug("Username does not match");
        ok(resp, "deny");
      }
      return;
    }

    introspect(password)
        .subscribe(
            () -> {
              if (introspectedCache.get(password).getString("consumer").equalsIgnoreCase(username))
                ok(resp, "allow");
              else {
                logger.debug("username does not match");
                ok(resp, "deny");
              }
            },
            err -> {
              logger.debug("Error=" + err.getMessage());
              ok(resp, "deny");
            });
  }

  public void authVhost(RoutingContext context) {
    HttpServerResponse resp = context.response();
    ok(resp, "allow");
  }

  public void authTopic(RoutingContext context) {
    HttpServerResponse resp = context.response();
    HttpServerRequest request = context.request();

    String username = request.getParam("username");
    String resourceType = request.getParam("resource");
    String resourceName = request.getParam("name");
    String permission = request.getParam("permission");
    String routingKey = request.getParam("routing_key");

    if ("configure".equalsIgnoreCase(permission) || "read".equalsIgnoreCase(permission)) {
      ok(resp, "deny");
      return;
    }

    if (!"exchange".equals(resourceType)) {
      ok(resp, "deny");
      return;
    }

    if (!resourceName.equalsIgnoreCase("EXCHANGE")) {
      ok(resp, "deny");
      return;
    }

    JsonArray authorisedRequests = usernameCache.get(username).getJsonArray("request");

    for (int i = 0; i < authorisedRequests.size(); i++) {
      JsonObject requestObject = authorisedRequests.getJsonObject(i);

      if (routingKey.equalsIgnoreCase(requestObject.getString("id"))) {
        ok(resp, "allow");
        return;
      }
    }

    ok(resp, "deny");
  }

  public void authResource(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String username = request.getParam("username");
    String resourceType = request.getParam("resource");
    String resourceName = request.getParam("name");
    String permission = request.getParam("permission");

    logger.debug(resourceName);
    logger.debug(username);
    logger.debug(resourceType);
    logger.debug(permission);

    if ("configure".equalsIgnoreCase(permission) || "read".equalsIgnoreCase(permission)) {
      ok(resp, "deny");
      return;
    }

    if (!"exchange".equals(resourceType)) {
      ok(resp, "deny");
      return;
    }

    if (!resourceName.equalsIgnoreCase("EXCHANGE")) {
      ok(resp, "deny");
      return;
    }

    ok(resp, "allow");
  }

  public boolean isStringSafe(String resource) {
    logger.debug("In is_string_safe");

    logger.debug("resource=" + resource);

    boolean safe =
        (resource.length() - (resource.replaceAll("[^a-zA-Z0-9-_./@]+", "")).length()) == 0;

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

    WebClientOptions options =
        new WebClientOptions()
            .setSsl(true)
            .setKeyStoreOptions(
                new JksOptions()
                    .setPath("certs/resource-server-keystore.jks")
                    .setPassword("password"));

    WebClient client = WebClient.create(vertx, options);

    return client
        .post(443, "auth.iudx.org.in", "/auth/v1/token/introspect")
        .ssl(true)
        .putHeader("content-type", "application/json")
        .rxSendJsonObject(body)
        .flatMapCompletable(
            response -> {
              if (response.statusCode() != 200)
                return Completable.error(new Throwable("Unauthorised"));
              else {
                introspectedCache.put(token, response.bodyAsJsonObject());
                usernameCache.put(token, response.bodyAsJsonObject());
                return Completable.complete();
              }
            });
  }
}
