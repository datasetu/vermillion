package vermillion.http;

import com.google.common.hash.Hashing;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.commons.lang3.RandomStringUtils;
import vermillion.Utils;
import vermillion.broker.reactivex.BrokerService;
import vermillion.database.reactivex.DbService;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.ConflictThrowable;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HttpServerVerticle extends AbstractVerticle {
  public final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);
  // HTTP Codes
  public final int OK = 200;
  public final int CREATED = 201;
  public final int ACCEPTED = 202;
  public final int BAD_REQUEST = 400;
  public final int FORBIDDEN = 403;
  public final int CONFLICT = 409;
  public final int INTERNAL_SERVER_ERROR = 500;

  public String schema;
  public String message;

  // Service Proxies
  public DbService dbService;
  public BrokerService brokerService;

  // Connection pool to speed up publish rates
  public Map<String, Channel> pool;

  // Flag to indicate whether an entity is autonomous
  public boolean autonomous;

  @Override
  public void start(Promise<Void> startPromise) {
    logger.debug("In start");

    int port = 8443;
    pool = new ConcurrentHashMap<String, Channel>();
    dbService = vermillion.database.DbService.createProxy(vertx.getDelegate(), "db.queue");
    brokerService =
        vermillion.broker.BrokerService.createProxy(vertx.getDelegate(), "broker.queue");

    Router router = Router.router(vertx);

    router.post("/entity/publish").handler(this::publish);
    router.get("/entity/subscribe").handler(this::subscribe);
    router.get("/catalogue").handler(this::cat);
    router.get("/owner/entities").handler(this::entities);
    router.post("/owner/reset-apikey").handler(this::resetApikey);
    router.post("/owner/set-autonomous").handler(this::setAutonomous);
    router.get("/admin/owners").handler(this::getOwners);

    router.post("/admin/register-owner").handler(this::registerOwner);
    router.post("/admin/deregister-owner").handler(this::deRegisterOwner);

    router.post("/admin/register-entity").handler(this::register);
    router.post("/owner/register-entity").handler(this::register);

    router.post("/admin/deregister-entity").handler(this::deRegister);
    router.post("/owner/deregister-entity").handler(this::deRegister);

    router.post("/owner/block").handler(this::block);
    router.post("/entity/block").handler(this::block);

    router.post("/entity/unblock").handler(this::block);
    router.post("/owner/unblock").handler(this::block);

    router.post("/entity/bind").handler(this::queueBind);
    router.post("/owner/bind").handler(this::queueBind);

    router.post("/entity/unbind").handler(this::queueBind);
    router.post("/owner/unbind").handler(this::queueBind);

    router.post("/entity/follow").handler(this::follow);
    router.post("/owner/follow").handler(this::follow);

    router.post("/entity/unfollow").handler(this::unfollow);
    router.post("/owner/unfollow").handler(this::unfollow);

    router.post("/entity/share").handler(this::share);
    router.post("/owner/share").handler(this::share);

    router.get("/entity/follow-requests").handler(this::followRequests);
    router.get("/owner/follow-requests").handler(this::followRequests);

    router.get("/entity/follow-status").handler(this::followStatus);
    router.get("/owner/follow-status").handler(this::followStatus);

    router.post("/entity/reject-follow").handler(this::rejectFollow);
    router.post("/owner/reject-follow").handler(this::rejectFollow);

    router.get("/entity/permissions").handler(this::permissions);
    router.get("/owner/permissions").handler(this::permissions);

    vertx
        .createHttpServer(
            new HttpServerOptions()
                .setSsl(true)
                .setKeyStoreOptions(
                    new JksOptions().setPath("my-keystore.jks").setPassword("password")))
        .requestHandler(router)
        .rxListen(port)
        .subscribe(
            s -> {
              logger.debug("Server started");
              startPromise.complete();
            },
            err -> {
              logger.debug("Could not start server. Cause=" + err.getMessage());
              startPromise.fail(err.getMessage());
            });

    vertx.exceptionHandler(Throwable::printStackTrace);

    brokerService
        .rxCreateQueue("DATABASE")
        .doOnError(err -> logger.error("Could not create queue. Cause=" + err.getMessage()))
        .subscribe();
  }

  public Channel getChannel(String id, String apikey) throws Exception {
    String token = id + ":" + apikey;

    if (!pool.containsKey(token) || !pool.get(token).isOpen()) {
      ConnectionFactory factory = new ConnectionFactory();

      factory.setUsername(id);
      factory.setPassword(apikey);
      factory.setVirtualHost("/");
      factory.setHost(Utils.getBrokerUrl(id));
      factory.setPort(5672);
      factory.setAutomaticRecoveryEnabled(true);
      factory.setNetworkRecoveryInterval(10000);

      Connection connection = factory.newConnection();
      Channel channel = connection.createChannel();

      logger.debug("Rabbitmq channel created");

      pool.put(id + ":" + apikey, channel);
    }

    return pool.get(id + ":" + apikey);
  }

  public void registerOwner(RoutingContext context) {
    logger.debug("In register owner");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String owner_name = request.getHeader("owner");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("owner=" + owner_name);

    if ((id == null) || (apikey == null) || (owner_name == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (!"admin".equalsIgnoreCase(id)) {
      apiFailure(context, new UnauthorisedThrowable("Only admins can invoke this API"));
      return;
    }

    if (!isValidOwner(owner_name)) {
      apiFailure(context, new UnauthorisedThrowable("Owner name is invalid"));
      return;
    }

    JsonObject responseJson = new JsonObject().put("id", owner_name);

    checkLogin(id, apikey)
        .andThen(Completable.defer(() -> checkEntityExistence(owner_name, false)))
        .andThen(Completable.defer(() -> brokerService.rxCreateOwnerResources(owner_name)))
        .andThen(Completable.defer(() -> brokerService.rxCreateOwnerBindings(owner_name)))
        .andThen(Single.defer(() -> generateCredentials(owner_name, "{}", "true")))
        .subscribe(
            generatedApikey -> {
              resp.putHeader("content-type", "application/json")
                  .setStatusCode(CREATED)
                  .end(responseJson.put("apikey", generatedApikey).encodePrettily());
            },
            err -> apiFailure(context, err));
  }

  // TODO: Allow owner deregistration only after all entities have been removed

  public void deRegisterOwner(RoutingContext context) {
    logger.debug("In deregister_owner");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String owner_name = request.getHeader("owner");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("owner=" + owner_name);

    if ((id == null) || (apikey == null) || (owner_name == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (!"admin".equalsIgnoreCase(id)) {
      apiFailure(context, new UnauthorisedThrowable("Only admin can invoke this API"));
      return;
    }

    if (!isValidOwner(owner_name)) {
      apiFailure(context, new BadRequestThrowable("Owner name is invalid"));
      return;
    }

    String acl_query =
        "DELETE FROM acl WHERE"
            + " from_id LIKE '"
            + owner_name
            + "/%'"
            + " OR exchange LIKE '"
            + owner_name
            + "/%'";

    String entity_query = "SELECT * FROM users WHERE" + " id LIKE '" + owner_name + "/%'";

    String user_query =
        "DELETE FROM users WHERE"
            + " id LIKE '"
            + owner_name
            + "/%'"
            + " OR id LIKE '"
            + owner_name
            + "'";

    checkLogin(id, apikey)
        .andThen(Completable.defer(() -> checkEntityExistence(owner_name, true)))
        .andThen(Completable.defer(() -> brokerService.rxDeleteOwnerResources(owner_name)))
        .andThen(Completable.defer(() -> dbService.rxRunQuery(acl_query)))
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(entity_query)))
        .flatMapPublisher(Flowable::fromIterable)
        .map(row -> processRow(row)[0])
        .collect(JsonArray::new, JsonArray::add)
        .flatMapCompletable(
            idList -> Completable.defer(() -> brokerService.rxDeleteEntityResources(idList)))
        .andThen(Completable.defer(() -> dbService.rxRunQuery(user_query)))
        .subscribe(() -> ok(resp), err -> apiFailure(context, err));
  }

  public void register(RoutingContext context) {
    logger.debug("In register entity");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String entity = request.getHeader("entity");
    String is_autonomous = request.getHeader("is-autonomous");
    String full_entity_name = id + "/" + entity;

    if ((!"true".equals(is_autonomous))
        && (!"false".equals(is_autonomous))
        && (is_autonomous != null)) {
      apiFailure(context, new BadRequestThrowable("Invalid is-autonomous header"));
      return;
    }

    String autonomous_flag =
        ((is_autonomous == null) || ("false".equals(is_autonomous))) ? "f" : "t";

    logger.debug(
        "id="
            + id
            + "\napikey="
            + apikey
            + "\nentity="
            + entity
            + "\nis-autonomous="
            + is_autonomous);

    // TODO: Check if body is null
    request.bodyHandler(
        body -> {
          schema = body.toString();
          logger.debug("schema=" + schema);

          try {
            new JsonObject(schema);
          } catch (Exception e) {
            apiFailure(context, new UnauthorisedThrowable("Body must be a valid JSON"));
            return;
          }

          if ((id == null) || (apikey == null) || (entity == null)) {
            apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
            return;
          }

          // TODO: Add appropriate field checks. E.g. valid owner, valid entity etc.
          // Check if ID is owner
          if (!isValidOwner(id)) {
            logger.debug("owner is invalid");
            apiFailure(context, new UnauthorisedThrowable("Invalid owner"));
            return;
          }

          if (!isStringSafe(entity)) {
            logger.debug("invalid entity name");
            apiFailure(context, new BadRequestThrowable("Invalid entity name"));
            return;
          }

          JsonObject responseJson = new JsonObject().put("id", full_entity_name);

          checkLogin(id, apikey)
              .andThen(Completable.defer(() -> checkEntityExistence(full_entity_name, false)))
              .andThen(
                  Completable.defer(() -> brokerService.rxCreateEntityResources(full_entity_name)))
              .andThen(
                  Completable.defer(() -> brokerService.rxCreateEntityBindings(full_entity_name)))
              .andThen(
                  Single.defer(
                      () -> generateCredentials(full_entity_name, schema, autonomous_flag)))
              .subscribe(
                  generatedApikey ->
                      resp.putHeader("content-type", "application/json")
                          .setStatusCode(CREATED)
                          .end(responseJson.put("apikey", generatedApikey).encodePrettily()),
                  err -> apiFailure(context, err));
        });
  }

  public void deRegister(RoutingContext context) {
    logger.debug("In deregister entity");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String entity = request.getHeader("entity");

    logger.debug("id=" + id + "\napikey=" + apikey + "\nentity=" + entity);

    // Check if ID is owner
    if (!isValidOwner(id)) {
      apiFailure(context, new UnauthorisedThrowable("Invalid owner"));
      return;
    }

    if (!isOwner(id, entity)) {
      apiFailure(context, new UnauthorisedThrowable("You are not the owner of the entity"));
      return;
    }

    if (!isValidEntity(entity)) {
      apiFailure(context, new UnauthorisedThrowable("Invalid entity"));
      return;
    }

    JsonArray entityArray = new JsonArray();

    String acl_query =
        "DELETE FROM acl WHERE " + "from_id = '" + entity + "' OR exchange LIKE '" + entity + ".%'";

    String follow_query =
        "DELETE FROM follow WHERE "
            + " requested_by = '"
            + entity
            + "' OR exchange LIKE '"
            + entity
            + ".%'";

    String user_query = "DELETE FROM users WHERE " + " id = '" + entity + "'";

    checkLogin(id, apikey)
        .andThen(Completable.defer(() -> checkEntityExistence(entity, true)))
        .andThen(
            Completable.defer(() -> brokerService.rxDeleteEntityResources(entityArray.add(entity))))
        .andThen(Completable.defer(() -> dbService.rxRunQuery(acl_query)))
        .andThen(Completable.defer(() -> dbService.rxRunQuery(follow_query)))
        .andThen(Completable.defer(() -> dbService.rxRunQuery(user_query)))
        .subscribe(() -> ok(resp), err -> apiFailure(context, err));
  }

  public void block(RoutingContext context) {
    logger.debug("In block API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String owner = request.getHeader("owner");
    String entity = request.getHeader("entity");

    String[] currentRoute = context.normalisedPath().split("/");

    String blocked = currentRoute[2].equals("block") ? "t" : "f";

    if ((id == null) || (apikey == null) || (owner == null) && (entity == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);

    if (owner != null) {
      if (!isStringSafe(owner)) {
        apiFailure(context, new BadRequestThrowable("Invalid owner"));
        return;
      }

      if (!("admin".equals(id))) {
        apiFailure(context, new UnauthorisedThrowable("Only admin can block owners"));
        return;
      }

      if (!isValidOwner(owner)) {
        apiFailure(context, new BadRequestThrowable("Owner is not valid"));
        return;
      }
    } else if (entity != null) {
      // TODO Verify the boolean logic here
      if (!(isOwner(id, entity) || "admin".equals(id))) {
        apiFailure(context, new UnauthorisedThrowable("You are not the owner of the entity"));
        return;
      }

      if (!isStringSafe(entity)) {
        apiFailure(context, new BadRequestThrowable("Invalid entity"));
        return;
      }

      if (!isValidEntity(entity)) {
        apiFailure(context, new UnauthorisedThrowable("Entity is not valid"));
        return;
      }
    }

    final String username = (owner == null) ? entity : owner;
    final String userString = (owner == null) ? entity : owner + "/%";

    String query =
        "UPDATE users SET blocked = '"
            + blocked
            + "' WHERE (id = '"
            + username
            + "' OR id LIKE '"
            + userString
            + "')";

    checkLogin(id, apikey)
        .andThen(Completable.defer(() -> checkEntityExistence(username, true)))
        .andThen(Completable.defer(() -> dbService.rxRunQuery(query)))
        .subscribe(() -> ok(resp), err -> apiFailure(context, err));
  }

  public void queueBind(RoutingContext context) {
    logger.debug("In queue_bind");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    // Mandatory headers
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String to = request.getHeader("to");
    String topic = request.getHeader("topic");
    String message_type = request.getHeader("message-type");

    // Optional headers
    String is_priority = request.getHeader("is-priority");
    String from = request.getHeader("from");

    String[] currentRoute = context.normalisedPath().split("/");

    logger.debug("Path=" + context.normalisedPath());

    String operation = currentRoute[2].equals("bind") ? "bind" : "unbind";

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("to=" + to);
    logger.debug("topic=" + topic);
    logger.debug("message-type=" + message_type);

    logger.debug("is-priorty=" + is_priority);
    logger.debug("from=" + from);

    if ((id == null)
        || (apikey == null)
        || (to == null)
        || (topic == null)
        || (message_type == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (isValidOwner(id) == isValidEntity(id)) {
      apiFailure(context, new BadRequestThrowable("Invalid id"));
      return;
    }

    if (isValidOwner(id)) {
      if (from == null) {
        apiFailure(context, new BadRequestThrowable("'from value missing in headers"));
        return;
      }

      if (!isOwner(id, from)) {
        apiFailure(
            context, new UnauthorisedThrowable("You are not the owner of the 'from' entity"));
        return;
      }

      if (!isValidEntity(from)) {
        apiFailure(context, new BadRequestThrowable("'from is not a valid entity"));
        return;
      }
    } else {
      from = id;
    }

    if ((!"public".equals(message_type))
        && (!"private".equals(message_type))
        && (!"protected".equals(message_type))
        && (!"diagnostics".equals(message_type))) {
      apiFailure(context, new BadRequestThrowable("'message-type' is invalid"));
      return;
    }

    if (("private".equals(message_type)) && (!isOwner(id, to))) {
      apiFailure(context, new UnauthorisedThrowable("You are not the owner of the 'to' entity"));
      return;
    }

    if ((!isStringSafe(from)) || (!isStringSafe(to)) || (!isStringSafe(topic))) {
      apiFailure(context, new UnauthorisedThrowable("Invalid headers"));
      return;
    }

    String queue = from;

    if (is_priority != null) {
      if ((!"true".equals(is_priority) && (!"false".equals(is_priority)))) {
        apiFailure(context, new UnauthorisedThrowable("Invalid 'is-priority' header"));
        return;
      } else if ("true".equals(is_priority)) {
        queue = queue + ".priority";
      }
    }

    final String from_id = from;
    final String exchange_name = to + "." + message_type;
    final String queue_name = queue;

    String acl_query =
        "SELECT * FROM acl WHERE"
            + " from_id	    ='"
            + from_id
            + "'"
            + " AND exchange  ='"
            + exchange_name
            + "'"
            + " AND permission='read'"
            + " AND valid_till > now()"
            + " AND topic	=   '"
            + topic
            + "'";

    autonomous = false;

    Completable loginCheck =
        Completable.defer(
            () ->
                checkLogin(id, apikey)
                    .andThen(
                        Completable.defer(
                            () ->
                                autonomous
                                    ? Completable.complete()
                                    : Completable.error(
                                        new UnauthorisedThrowable("Unauthorised")))));

    if ("public".equals(message_type) || isOwner(id, to)) {
      loginCheck
          .andThen(
              "bind".equals(operation)
                  ? brokerService.rxBind(queue_name, exchange_name, topic)
                  : brokerService.rxUnbind(queue_name, exchange_name, topic))
          .subscribe(() -> ok(resp), err -> apiFailure(context, err));
    } else {
      loginCheck
          .andThen(dbService.rxRunSelectQuery(acl_query))
          .flatMapCompletable(
              result ->
                  result.size() == 1
                      ? Completable.complete()
                      : Completable.error(new UnauthorisedThrowable("Unauthorised")))
          .andThen(
              "bind".equals(operation)
                  ? brokerService.rxBind(queue_name, exchange_name, topic)
                  : brokerService.rxUnbind(queue_name, exchange_name, topic))
          .subscribe(() -> ok(resp), err -> apiFailure(context, err));
    }
  }

  public void follow(RoutingContext context) {
    logger.debug("In follow API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    // Mandatory Headers
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String to = request.getHeader("to");
    String topic = request.getHeader("topic");
    String validity = request.getHeader("validity");
    String permission = request.getHeader("permission");

    // Optional Headers
    String from = request.getHeader("from");
    String message_type_header = request.getHeader("message-type");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("to=" + to);
    logger.debug("message-type=" + message_type_header);
    logger.debug("topic=" + topic);
    logger.debug("validity=" + validity);
    logger.debug("permission=" + permission);

    if ((id == null)
        || (apikey == null)
        || (to == null)
        || (topic == null)
        || (validity == null)
        || (permission == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (isValidOwner(id) == isValidEntity(id)) {
      apiFailure(context, new BadRequestThrowable("Invalid id"));
      return;
    }

    if (isValidOwner(id)) {
      if (from == null) {
        apiFailure(context, new BadRequestThrowable("'from value missing in headers"));
        return;
      }

      if (!isOwner(id, from)) {
        apiFailure(
            context, new UnauthorisedThrowable("You are not the owner of the 'from' entity"));
        return;
      }

      if (!isValidEntity(from)) {
        apiFailure(context, new BadRequestThrowable("'from is not a valid entity"));
        return;
      }
    } else {
      from = id;
    }

    if (message_type_header != null) {
      if ((!"protected".equals(message_type_header))
          && (!"diagnostics".equals(message_type_header))) {
        apiFailure(context, new BadRequestThrowable("'message-type is invalid"));
        return;
      }

    } else {
      message_type_header = "protected";
    }

    if (!(("read".equals(permission))
        || ("write".equals(permission))
        || ("read-write".equals(permission)))) {
      apiFailure(context, new BadRequestThrowable("Invalid permission string"));
      return;
    }

    try {
      int validity_integer = Integer.parseInt(validity);

      if ((validity_integer < 0) || (validity_integer > 10000)) {
        apiFailure(context, new BadRequestThrowable("Invalid validity header"));
        return;
      }
    } catch (Exception e) {
      apiFailure(context, new BadRequestThrowable("Invalid validity header"));
      return;
    }

    if ((!isStringSafe(from))
        || (!isStringSafe(to))
        || (!isStringSafe(topic))
        || (!isStringSafe(validity))
        || (!isStringSafe(permission))) {
      apiFailure(context, new BadRequestThrowable("Invalid headers"));
      return;
    }

    final String from_id = from;
    final String message_type = message_type_header;
    final String status = (isOwner(id, to)) ? "approved" : "pending";

    String validity_string = "now()	+   interval	'" + validity + " hours'";

    autonomous = false;
    JsonObject responseJson = new JsonObject();

    Completable loginCheck =
        Completable.defer(
            () ->
                checkLogin(id, apikey)
                    .andThen(
                        Completable.defer(
                            () ->
                                autonomous
                                    ? Completable.complete()
                                    : Completable.error(
                                        new UnauthorisedThrowable("Unauthorised")))));

    // TODO: If follow request exists, return follow-id without processing anything
    loginCheck
        .andThen(Completable.defer(() -> checkEntityExistence(to, true)))
        .andThen(Completable.defer(() -> checkEntityExistence(from_id, true)))
        .andThen(Observable.fromIterable(Arrays.asList(permission.split("-"))))
        .flatMapCompletable(
            currentPermission ->
                insertIntoFollow(
                        id,
                        to + ("read".equals(currentPermission) ? "." + message_type : ".command"),
                        topic,
                        currentPermission,
                        validity,
                        from_id,
                        status)
                    .map(
                        followId -> {
                          responseJson.put("follow-id-" + currentPermission, followId);
                          return followId;
                        })
                    .flatMapCompletable(
                        followId ->
                            "approved".equals(status)
                                ? insertIntoAcl(
                                    from_id,
                                    to
                                        + ("read".equals(currentPermission)
                                            ? "." + message_type
                                            : ".command"),
                                    currentPermission,
                                    validity_string,
                                    followId,
                                    topic)
                                : Completable.complete())
                    .andThen(
                        ("approved".equals(status)) && ("write".equals(currentPermission))
                            ? bindToPublishExchange(from_id, to, topic)
                            : Completable.complete()))
        .andThen(
            Completable.defer(() -> publishToNotification(from_id, permission, to, autonomous)))
        .subscribe(
            () ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(ACCEPTED)
                    .end(responseJson.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public Completable bindToPublishExchange(String from, String to, String topic) {

    String exchange = from + ".publish";
    String queue = to + ".command";
    String routingKey = to + ".command." + topic;

    return brokerService.rxBind(queue, exchange, routingKey);
  }

  public Completable publishToNotification(
      String from_id, String permission, String to, boolean autonomous) {

    String exchange = autonomous ? to + ".notification" : to.split("/")[0] + ".notification";
    String topic = "Request for follow";
    String message_string = from_id + " has requested " + permission + " access on " + to;
    JsonObject message = new JsonObject().put("message", message_string);

    return Completable.defer(
        () -> brokerService.rxAdminPublish(exchange, topic, message.toString()));
  }

  public Single<String> insertIntoFollow(
      String id,
      String exchange,
      String topic,
      String permission,
      String validity,
      String from,
      String status) {

    logger.debug("In insert into follow");
    logger.debug("id=" + id);
    logger.debug("exchange=" + exchange);
    logger.debug("topic=" + topic);
    logger.debug("permission=" + permission);
    logger.debug("validity=" + validity);
    logger.debug("from=" + from);
    logger.debug("status=" + status);

    String follow_query =
        "INSERT INTO follow VALUES (DEFAULT, '"
            + id
            + "','"
            + exchange
            + "',"
            + "now(), '"
            + permission
            + "','"
            + topic
            + "','"
            + validity
            + "','"
            + status
            + "','"
            + from
            + "')";

    String follow_id_query =
        "SELECT * FROM follow WHERE from_id	=   '" + from + "' AND exchange =	'" + exchange + "'";

    return dbService
        .rxRunQuery(follow_query)
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(follow_id_query)))
        .map(row -> row.get(0))
        .map(row -> processRow(row)[0]);
  }

  public Completable insertIntoAcl(
      String from_id,
      String exchange,
      String permission,
      String valid_till,
      String follow_id,
      String topic) {
    logger.debug("In insert into acl");

    String acl_query =
        "INSERT INTO acl VALUES (	'"
            + from_id
            + "','"
            + exchange
            + "','"
            + permission
            + "',"
            + valid_till
            + ",'"
            + follow_id
            + "','"
            + topic
            + "',DEFAULT)";

    return dbService.rxRunQuery(acl_query);
  }

  public void unfollow(RoutingContext context) {
    logger.debug("In unfollow API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    // Mandatory Headers
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String to = request.getHeader("to");
    String topic = request.getHeader("topic");
    String permission = request.getHeader("permission");
    String message_type_header = request.getHeader("message-type");

    // Optional Headers
    String from = request.getHeader("from");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("to=" + to);
    logger.debug("message-type=" + message_type_header);
    logger.debug("topic=" + topic);
    logger.debug("permission=" + permission);

    if ((id == null)
        || (apikey == null)
        || (to == null)
        || (topic == null)
        || (permission == null)) {
      logger.debug("Inputs missing in headers");
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (isValidOwner(id) == isValidEntity(id)) {
      logger.debug("Invalid id");
      apiFailure(context, new BadRequestThrowable("Invalid id"));
      return;
    }

    if (isValidOwner(id)) {
      if (from == null) {
        logger.debug("'from' value missing in headers");
        apiFailure(context, new BadRequestThrowable("'from' value missing in headers"));
        return;
      }

      if (!isOwner(id, from)) {

        logger.debug("You are not the owner of the 'from' entity");
        apiFailure(
            context, new UnauthorisedThrowable("You are not the owner of the 'from' entity"));
        return;
      }

      if (!isValidEntity(from)) {
        logger.debug("'from' is not a valid entity");
        apiFailure(context, new BadRequestThrowable("'from' is not a valid entity"));
        return;
      }
    } else {
      from = id;
    }

    if (message_type_header != null) {
      if ((!"protected".equals(message_type_header))
          && (!"diagnostics".equals(message_type_header))) {
        logger.debug("'message-type' is invalid");
        apiFailure(context, new BadRequestThrowable("'message-type' is invalid"));
        return;
      }

    } else {
      message_type_header = "protected";
    }

    if (!(("read".equals(permission))
        || ("write".equals(permission))
        || ("read-write".equals(permission)))) {
      logger.debug("Invalid permission string");
      apiFailure(context, new BadRequestThrowable("Invalid permission string"));
      return;
    }

    if ((!isStringSafe(from)) || (!isStringSafe(to)) || (!isStringSafe(topic))) {
      logger.debug("Invalid headers");
      apiFailure(context, new BadRequestThrowable("Invalid headers"));
      return;
    }

    final String from_id = from;
    final String message_type = message_type_header;
    final String exchange = to + "." + message_type;

    String acl_query =
        "SELECT * FROM acl WHERE from_id  = '"
            + from_id
            + "' AND exchange	=	'"
            + to
            + "."
            + "%1$s"
            + "' AND topic		=	'"
            + topic
            + "' AND permission = '%2$s'";

    String delete_query = "DELETE FROM %1$s WHERE follow_id='%2$s'";

    autonomous = false;

    Completable loginCheck =
        Completable.defer(
            () ->
                checkLogin(id, apikey)
                    .andThen(
                        Completable.defer(
                            () ->
                                autonomous
                                    ? Completable.complete()
                                    : Completable.error(
                                        new UnauthorisedThrowable("Unauthorised")))));

    Completable writeUnbind =
        Completable.defer(
            () ->
                brokerService.rxUnbind(
                    to + ".command", from_id + ".publish", to + ".command." + topic));

    Completable readUnbind =
        Completable.defer(() -> brokerService.rxUnbind(from_id, exchange, topic));

    Completable priorityUnbind =
        Completable.defer(() -> brokerService.rxUnbind(from_id + ".priority", exchange, topic));

    loginCheck
        .andThen(Observable.fromIterable(Arrays.asList(permission.split("-"))))
        .flatMapCompletable(
            currentPermission ->
                dbService
                    .rxRunSelectQuery(
                        String.format(
                            acl_query,
                            ("write".equals(currentPermission) ? "command" : message_type),
                            currentPermission))
                    .map(row -> row.size() == 1 ? row.get(0) : "")
                    .map(
                        queryResult ->
                            (queryResult.length() != 0)
                                ? processRow(queryResult.toString())[4]
                                : "")
                    .flatMapCompletable(
                        followId ->
                            (followId.length() != 0)
                                ? dbService
                                    .rxRunQuery(String.format(delete_query, "acl", followId))
                                    .concatWith(
                                        dbService.rxRunQuery(
                                            String.format(delete_query, "follow", followId)))
                                : Completable.error(
                                    new UnauthorisedThrowable("No such entry in ACL")))
                    .andThen("write".equals(currentPermission) ? writeUnbind : readUnbind)
                    .andThen(
                        "read".equals(currentPermission) ? priorityUnbind : Completable.complete()))
        .subscribe(() -> ok(resp), err -> apiFailure(context, err));
  }

  public void share(RoutingContext context) {
    logger.debug("In share API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String follow_id = request.getHeader("follow-id");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("follow-id=" + follow_id);

    if ((id == null) || (apikey == null) || (follow_id == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    String exchange_string =
        isValidOwner(id) ? (id + "/%.%") : (isValidEntity(id)) ? id + ".%" : "";

    if (!isStringSafe(follow_id)) {
      apiFailure(context, new BadRequestThrowable("Invalid follow-id"));
      return;
    }

    String follow_query =
        "SELECT * FROM follow WHERE follow_id   =	"
            + Integer.parseInt(follow_id)
            + " AND exchange LIKE 			'"
            + exchange_string
            + "' AND status   =	'pending'";

    String update_follow_query =
        "UPDATE follow SET status = 'approved' WHERE follow_id = " + follow_id;

    String acl_insert =
        "INSERT INTO acl VALUES("
            + "'"
            + "%1$s"
            + "','"
            + "%2$s"
            + "','"
            + "%3$s"
            + "',"
            + "now() + interval '"
            + "%4$s"
            + " hours'"
            + ",'"
            + follow_id
            + "','"
            + "%5$s"
            + "',"
            + "DEFAULT)";

    autonomous = false;

    Completable loginCheck =
        Completable.defer(
            () ->
                checkLogin(id, apikey)
                    .andThen(
                        Completable.defer(
                            () ->
                                autonomous
                                    ? Completable.complete()
                                    : Completable.error(
                                        new UnauthorisedThrowable("Unauthorised")))));

    loginCheck
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(follow_query)))
        .map(result -> (result.size() == 1) ? processRow(result.get(0)) : new String[0])
        .flatMapCompletable(
            row ->
                (row.length != 0)
                    ? dbService
                        .rxRunQuery(update_follow_query)
                        /**
                         * from_id = row[8], exchange = row[2], permission = row[4], topic = row[5],
                         * validity = row[6]
                         */
                        .concatWith(
                            dbService.rxRunQuery(
                                String.format(acl_insert, row[8], row[2], row[4], row[6], row[5])))
                        .andThen(
                            "write".equals(row[4])
                                ? brokerService.rxBind(
                                    row[2], row[8] + ".publish", row[2] + "." + row[5])
                                : Completable.complete())
                    : Completable.error(
                        new UnauthorisedThrowable(
                            "Follow ID is invalid or has already been approved")))
        .subscribe(() -> ok(resp), err -> apiFailure(context, err));
  }

  public void cat(RoutingContext context) {

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    logger.debug("In cat API");

    String cat_query = "SELECT id, schema FROM users WHERE id LIKE '%/%'";

    dbService
        .rxRunSelectQuery(cat_query)
        .flatMapPublisher(Flowable::fromIterable)
        .map(this::processRow)
        // id = row[0], schema = row[1]
        .map(row -> new JsonObject().put("id", row[0]).put("schema", new JsonObject(row[1])))
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public void entities(RoutingContext context) {
    logger.debug("In entities API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);

    if ((id == null) || (apikey == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (!isValidOwner(id)) {
      apiFailure(context, new BadRequestThrowable("'id' is not valid"));
      return;
    }

    String users_query = "SELECT * FROM users WHERE id LIKE '" + id + "/%'";

    checkLogin(id, apikey)
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(users_query)))
        .flatMapPublisher(Flowable::fromIterable)
        .map(this::processRow)
        // id = row[0], is-autonomous = row[5]
        .map(row -> new JsonObject().put("id", row[0]).put("is-autonomous", row[5]))
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public void resetApikey(RoutingContext context) {
    logger.debug("In reset apikey");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String owner = request.getHeader("owner");
    String entity = request.getHeader("entity");

    if ((id == null) || (apikey == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);

    if ((owner == null) && (entity == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (owner != null) {
      if (!isStringSafe(owner)) {
        apiFailure(context, new BadRequestThrowable("Invalid owner"));
        return;
      }

      if (!isValidOwner(owner)) {
        apiFailure(context, new UnauthorisedThrowable("Invalid owner"));
        return;
      }

      if (!("admin".equals(id))) {
        apiFailure(context, new UnauthorisedThrowable("Only admin can reset owners' credentials"));
        return;
      }
    } else if (entity != null) {
      if (!(isOwner(id, entity) || "admin".equals(id))) {
        apiFailure(context, new UnauthorisedThrowable("You are not the owner of the entity"));
        return;
      }

      if (!isStringSafe(entity)) {
        apiFailure(context, new BadRequestThrowable("Invalid entity"));
        return;
      }

      if (!isValidEntity(entity)) {
        apiFailure(context, new BadRequestThrowable("Entity is not valid"));
        return;
      }
    }

    final String username = (owner == null) ? entity : owner;

    autonomous = false;

    Completable loginCheck =
        checkLogin(id, apikey)
            .andThen(
                Completable.defer(
                    () ->
                        autonomous
                            ? Completable.complete()
                            : Completable.error(new UnauthorisedThrowable("Unauthorised"))));

    loginCheck
        .andThen(Completable.defer(() -> checkEntityExistence(username, true)))
        .andThen(Single.defer(() -> updateCredentials(username)))
        .map(updatedApikey -> new JsonObject().put("id", username).put("apikey", updatedApikey))
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public void setAutonomous(RoutingContext context) {
    logger.debug("In set autonomous");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String entity = request.getHeader("entity");
    String autonomous = request.getHeader("is-autonomous");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("entity=" + entity);
    logger.debug("is-autonomous=" + autonomous);

    if ((id == null) || (apikey == null) || (entity == null) || (autonomous == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (!isValidOwner(id)) {
      apiFailure(context, new BadRequestThrowable("id is not valid"));
      return;
    }

    if (!isValidEntity(entity)) {
      apiFailure(context, new BadRequestThrowable("entity is not valid"));
      return;
    }

    if (!(("true".equals(autonomous)) || ("false".equals(autonomous)))) {
      apiFailure(context, new BadRequestThrowable("Invalid is-autonomous header"));
      return;
    }

    if (!isOwner(id, entity)) {
      apiFailure(context, new UnauthorisedThrowable("You are not the owner of the entity"));
      return;
    }

    String update_query =
        "UPDATE users SET is_autonomous	= '" + autonomous + "' WHERE id	=  '" + entity + "'";

    checkLogin(id, apikey)
        .andThen(Completable.defer(() -> dbService.rxRunQuery(update_query)))
        .subscribe(() -> ok(resp), err -> apiFailure(context, err));
  }

  public void getOwners(RoutingContext context) {
    logger.debug("In owners API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);

    if (!"admin".equals(id)) {
      apiFailure(context, new UnauthorisedThrowable("Only admin can invoke this API"));
      return;
    }

    String user_query = "SELECT * FROM users WHERE id NOT LIKE '%/%'";

    checkLogin(id, apikey)
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(user_query)))
        .flatMapPublisher(Flowable::fromIterable)
        .map(this::processRow)
        .map(row -> row[0])
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public void followRequests(RoutingContext context) {
    logger.debug("In follow-requests API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);

    if ((id == null) || (apikey == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (isValidOwner(id) == isValidEntity(id)) {
      apiFailure(context, new BadRequestThrowable("Invalid id"));
      return;
    }

    String exchange_string =
        isValidOwner(id) ? (id + "/%.%") : (isValidEntity(id)) ? id + ".%" : "";

    autonomous = false;

    Completable loginCheck =
        checkLogin(id, apikey)
            .andThen(
                Completable.defer(
                    () ->
                        autonomous
                            ? Completable.complete()
                            : Completable.error(new UnauthorisedThrowable("Unauthorised"))));

    String follow_query =
        "SELECT * FROM follow WHERE exchange LIKE '"
            + exchange_string
            + "' AND status = 'pending' ORDER BY TIME";

    loginCheck
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(follow_query)))
        .flatMapPublisher(Flowable::fromIterable)
        .map(this::processRow)
        .map(
            row ->
                new JsonObject()
                    .put("follow-id", row[0])
                    .put("from", row[1])
                    .put("to", row[2])
                    .put("time", row[3])
                    .put("permission", row[4])
                    .put("topic", row[5])
                    .put("validity", row[6]))
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public void followStatus(RoutingContext context) {
    logger.debug("In follow-status API");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);

    if ((id == null) || (apikey == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (isValidOwner(id) == isValidEntity(id)) {
      apiFailure(context, new BadRequestThrowable("Invalid id"));
      return;
    }

    String from_string = isValidOwner(id) ? (id + "/%") : (isValidEntity(id)) ? id : "";

    Completable loginCheck =
        checkLogin(id, apikey)
            .andThen(
                Completable.defer(
                    () ->
                        autonomous
                            ? Completable.complete()
                            : Completable.error(new UnauthorisedThrowable("Unauthorised"))));

    String follow_query =
        "SELECT * FROM follow WHERE from_id LIKE '" + from_string + "' ORDER BY TIME";

    loginCheck
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(follow_query)))
        .flatMapPublisher(Flowable::fromIterable)
        .map(this::processRow)
        .map(
            row ->
                new JsonObject()
                    .put("follow-id", row[0])
                    .put("from", row[1])
                    .put("to", row[2])
                    .put("time", row[3])
                    .put("permission", row[4])
                    .put("topic", row[5])
                    .put("validity", row[6])
                    .put("status", row[7]))
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public void rejectFollow(RoutingContext context) {
    logger.debug("In reject follow");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String follow_id = request.getHeader("follow-id");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("follow-id=" + follow_id);

    if ((id == null) || (apikey == null) || (follow_id == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (!isStringSafe(follow_id)) {
      apiFailure(context, new BadRequestThrowable("Invalid follow-id"));
      return;
    }

    String exchange_string =
        isValidOwner(id) ? (id + "/%.%") : (isValidEntity(id)) ? id + ".%" : "";

    String follow_query =
        "SELECT * FROM follow WHERE follow_id = '"
            + follow_id
            + "' AND exchange LIKE '"
            + exchange_string
            + "' AND status = 'pending'";

    String update_query =
        "UPDATE follow SET status	=   'rejected'" + "WHERE follow_id		=   '" + follow_id + "'";

    Completable loginCheck =
        checkLogin(id, apikey)
            .andThen(
                Completable.defer(
                    () ->
                        autonomous
                            ? Completable.complete()
                            : Completable.error(new UnauthorisedThrowable("Unauthorised"))));

    loginCheck
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(follow_query)))
        .flatMapCompletable(
            row ->
                (row.size() == 1)
                    ? Completable.complete()
                    : Completable.error(new BadRequestThrowable("Follow-id is invalid")))
        .andThen(Completable.defer(() -> dbService.rxRunQuery(update_query)))
        .subscribe(() -> ok(resp), err -> apiFailure(context, err));
  }

  public void permissions(RoutingContext context) {
    logger.debug("In permissions");

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String entity = request.getHeader("entity");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);

    if ((id == null) || (apikey == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (isValidOwner(id) == isValidEntity(id)) {
      apiFailure(context, new BadRequestThrowable("Invalid id"));
      return;
    }

    if (isValidOwner(id)) {
      if (entity == null) {
        apiFailure(context, new BadRequestThrowable("Entity value not specified in headers"));
        return;
      }

      if (!isOwner(id, entity)) {
        apiFailure(context, new UnauthorisedThrowable("You are not the owner of the entity"));
        return;
      }
    }

    String from_id = (isValidOwner(id)) ? entity : (isValidEntity(id) ? id : "");

    String acl_query =
        "SELECT * FROM acl WHERE from_id	=   '" + from_id + "' AND valid_till > now()";

    checkLogin(id, apikey)
        .andThen(Single.defer(() -> dbService.rxRunSelectQuery(acl_query)))
        .flatMapPublisher(Flowable::fromIterable)
        .map(this::processRow)
        .map(row -> new JsonObject().put("entity", row[1]).put("permission", row[2]))
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public void publish(RoutingContext context) {
    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");
    String to = request.getHeader("to");
    String subject = request.getHeader("subject");
    String message_type = request.getHeader("message-type");

    logger.debug("id=" + id);
    logger.debug("apikey=" + apikey);
    logger.debug("to=" + to);
    logger.debug("subject=" + subject);
    logger.debug("message-type=" + message_type);

    if ((id == null)
        || (apikey == null)
        || (to == null)
        || (subject == null)
        || (message_type == null)) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    // TODO: Add proper validation
    request.bodyHandler(
        body -> {
          message = body.toString();

          logger.debug("body=" + message);

          String temp_exchange = "";
          String temp_topic = "";

          if (id.equals(to)) {
            if ((!"public".equals(message_type))
                && (!"private".equals(message_type))
                && (!"protected".equals(message_type))
                && (!"diagnostics".equals(message_type))) {
              apiFailure(context, new BadRequestThrowable("'message-type' is invalid"));
              return;
            }

            temp_exchange = id + "." + message_type;
            temp_topic = subject;
          } else {
            if (!"command".equals(message_type)) {
              apiFailure(context, new BadRequestThrowable("'message-type can only be command"));
              return;
            }

            temp_topic = to + "." + message_type + "." + subject;
            temp_exchange = id + ".publish";
          }

          if (!isValidEntity(to)) {
            apiFailure(context, new BadRequestThrowable("'to' is not a valid entity"));
            return;
          }

          final String exchange = temp_exchange;
          final String topic = temp_topic;

          logger.debug("Exchange=" + exchange);
          logger.debug("Topic=" + topic);

          if (!pool.containsKey(id + ":" + apikey)) {
            logger.debug("Pool does not contain key");

            checkLogin(id, apikey)
                .doOnComplete(
                    () -> {
                      try {
                        getChannel(id, apikey)
                            .basicPublish(exchange, topic, null, message.getBytes());
                        accepted(resp);
                      } catch (Exception e) {
                        apiFailure(
                            context, new InternalErrorThrowable("Could not publish to broker"));
                      }
                    })
                .subscribe(() -> accepted(resp), err -> apiFailure(context, err));
          } else {
            try {
              getChannel(id, apikey).basicPublish(exchange, topic, null, message.getBytes());
              accepted(resp);
            } catch (Exception e) {
              apiFailure(context, new InternalErrorThrowable("Could not publish to broker"));
            }
          }
        });
  }

  public void subscribe(RoutingContext context) {

    HttpServerRequest request = context.request();
    HttpServerResponse resp = request.response();

    // Mandatory headers
    String id = request.getHeader("id");
    String apikey = request.getHeader("apikey");

    // Optional headers
    String message_type_header = request.getHeader("message-type");
    String num_messages = request.getHeader("num-messages");

    if (id == null || apikey == null) {
      apiFailure(context, new BadRequestThrowable("Inputs missing in headers"));
      return;
    }

    if (message_type_header != null) {
      if ((!"priority".equals(message_type_header))
          && (!"command".equals(message_type_header))
          && (!"notification".equals(message_type_header))
          && (!"private".equals(message_type_header))) {
        apiFailure(context, new BadRequestThrowable("'message-type' is invalid"));
        return;
      }

      message_type_header = "." + message_type_header;

    } else {
      message_type_header = "";
    }

    if (num_messages != null) {
      int messages = 0;

      try {
        messages = Integer.parseInt(num_messages);
      } catch (Exception e) {
        apiFailure(context, new BadRequestThrowable("Invalid num-messages header"));
        return;
      }

      if (messages < 0) {
        apiFailure(context, new BadRequestThrowable("Invalid num-messages header"));
        return;
      }

      if (messages > 1000) {
        num_messages = "1000";
      }
    } else {
      num_messages = "10";
    }

    final String message_type = message_type_header;
    final int message_count = Integer.parseInt(num_messages);

    checkLogin(id, apikey)
        .andThen(
            Single.defer(() -> brokerService.rxSubscribe(id, apikey, message_type, message_count)))
        .subscribe(
            response ->
                resp.putHeader("content-type", "application/json")
                    .setStatusCode(OK)
                    .end(response.encodePrettily()),
            err -> apiFailure(context, err));
  }

  public Completable checkEntityExistence(String registration_entity_id, boolean shouldExist) {
    logger.debug("in entity does not exist");

    String query = "SELECT * FROM users WHERE id = '" + registration_entity_id + "'";

    return dbService
        .rxRunSelectQuery(query)
        .map(row -> row.size() == 1)
        .map(size -> shouldExist == size)
        .flatMapCompletable(
            exists -> {
              if (exists) return Completable.complete();
              else if ((shouldExist) && (!exists))
                return Completable.error(new UnauthorisedThrowable("No such owner/entity"));
              else return Completable.error(new ConflictThrowable("Owner/Entity already present"));
            });
  }

  public boolean isOwner(String owner, String entity) {
    logger.debug("In is_owner");

    logger.debug("Owner=" + owner);
    logger.debug("Entity=" + entity);

    return (isValidOwner(owner)) && (entity.startsWith(owner)) && (entity.contains("/"));
  }

  public Single<String> generateCredentials(String id, String schema, String autonomous) {
    logger.debug("In generate credentials");

    String apikey = genRandString(32);
    String salt = genRandString(32);
    String blocked = "f";

    String string_to_hash = apikey + salt + id;
    String hash = Hashing.sha256().hashString(string_to_hash, StandardCharsets.UTF_8).toString();

    logger.debug("Id=" + id);
    logger.debug("Generated apikey=" + apikey);
    logger.debug("Salt=" + salt);
    logger.debug("String to hash=" + string_to_hash);
    logger.debug("Hash=" + hash);

    String query =
        "INSERT INTO users VALUES('"
            + id
            + "','"
            + hash
            + "','"
            + schema
            + "','"
            + salt
            + "','"
            + blocked
            + "','"
            + autonomous
            + "')";

    return dbService.rxRunQuery(query).andThen(Single.just(apikey));
  }

  public Single<String> updateCredentials(String id) {
    logger.debug("In update credentials");

    String apikey = genRandString(32);
    String salt = genRandString(32);
    String string_to_hash = apikey + salt + id;
    String hash = Hashing.sha256().hashString(string_to_hash, StandardCharsets.UTF_8).toString();

    logger.debug("Id=" + id);
    logger.debug("Generated apikey=" + apikey);
    logger.debug("Salt=" + salt);
    logger.debug("String to hash=" + string_to_hash);
    logger.debug("Hash=" + hash);

    String update_query =
        "UPDATE users SET password_hash =	'"
            + hash
            + "', salt    =	'"
            + salt
            + "' WHERE id =	'"
            + id
            + "'";

    return dbService.rxRunQuery(update_query).andThen(Single.just(apikey));
  }

  public String genRandString(int len) {
    logger.debug("In genRandString");

    // Characters for generating apikeys
    String PASSWORD_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-";
    String randStr =
        RandomStringUtils.random(
            len, 0, PASSWORD_CHARS.length(), true, true, PASSWORD_CHARS.toCharArray());

    logger.debug("Generated random string = " + randStr);

    return randStr;
  }

  public Completable checkLogin(String id, String apikey) {
    logger.debug("In check_login");

    logger.debug("ID=" + id);
    logger.debug("Apikey=" + apikey);

    if ("".equals(id) || "".equals(apikey)) {
      return Completable.error(new BadRequestThrowable("ID or Apikey is missing"));
    }

    if (isValidOwner(id) == isValidEntity(id)) {
      return Completable.error(new BadRequestThrowable("User is neither an owner nor an entity"));
    }

    String query = "SELECT * FROM users WHERE id	=	'" + id + "'" + " AND blocked = 'f'";

    return dbService
        .rxRunSelectQuery(query)
        .map(row -> row.get(0))
        .onErrorReturnItem("")
        .map(
            row -> {
              if (row.length() != 0) return Arrays.asList(processRow(row));
              else return Collections.singletonList("");
            })
        .map(
            row -> {
              if (row.size() > 1) {
                logger.debug("Row=" + row.toString());
                String salt = row.get(3);
                logger.debug("Salt=" + salt);
                String string_to_hash = apikey + salt + id;
                String expected_hash = row.get(1);
                logger.debug("Expected hash=" + expected_hash);
                String actual_hash =
                    Hashing.sha256().hashString(string_to_hash, StandardCharsets.UTF_8).toString();
                logger.debug("Actual hash=" + actual_hash);
                autonomous = "true".equals(row.get(5));
                logger.debug("Autonomous=" + autonomous);
                return expected_hash.equals(actual_hash);
              } else return false;
            })
        .flatMapCompletable(
            login ->
                login
                    ? Completable.complete()
                    : Completable.error(new UnauthorisedThrowable("Invalid id or apikey")));
  }

  public boolean isStringSafe(String resource) {
    logger.debug("In is_string_safe");

    logger.debug("resource=" + resource);

    boolean safe =
        (resource.length() - (resource.replaceAll("[^#-/a-zA-Z0-9-_.]+", "")).length()) == 0;

    logger.debug("Original resource name =" + resource);
    logger.debug("Replaced resource name =" + resource.replaceAll("[^#-/a-zA-Z0-9-_.]+", ""));
    return safe;
  }

  public boolean isValidOwner(String owner_name) {
    logger.debug("In is_valid_owner");

    // TODO simplify this
    if ((!Character.isDigit(owner_name.charAt(0)))
        && ((owner_name.length() - (owner_name.replaceAll("[^a-z0-9-_.]+", "")).length()) == 0)) {
      logger.debug("Original owner name = " + owner_name);
      logger.debug("Replaced name = " + owner_name.replaceAll("[^a-z0-9-_.]+", ""));
      return true;
    } else {
      logger.debug("Original owner name = " + owner_name);
      logger.debug("Replaced name = " + owner_name.replaceAll("[^a-z0-9-_.]+", ""));
      return false;
    }
  }

  public boolean isValidEntity(String resource) {
    // TODO: Add a length check
    logger.debug("In is_valid_entity");

    String[] entries = resource.split("/");

    logger.debug("Entries = " + Arrays.asList(entries));

    if (entries.length != 2) {
      return false;
    } else return (isValidOwner(entries[0])) && (isStringSafe(entries[1]));
  }

  public String[] processRow(String row) {
    logger.debug("Row=" + row);
    return row.substring(row.indexOf("[") + 1, row.indexOf("]")).trim().split(",\\s");
  }

  public void ok(HttpServerResponse resp) {
    if (!resp.closed()) {
      resp.setStatusCode(OK).end();
    }
  }

  public void accepted(HttpServerResponse resp) {
    if (!resp.closed()) {
      resp.setStatusCode(ACCEPTED).end();
    }
  }

  private void apiFailure(RoutingContext context, Throwable t) {
    logger.debug("In apifailure");
    logger.debug("Message=" + t.getMessage());
    if (t instanceof BadRequestThrowable) {
      context.response().setStatusCode(BAD_REQUEST).end(t.getMessage());
    } else if (t instanceof UnauthorisedThrowable) {
      context.response().setStatusCode(FORBIDDEN).end(t.getMessage());
    } else if (t instanceof ConflictThrowable) {
      context.response().setStatusCode(CONFLICT).end(t.getMessage());
    } else if (t instanceof InternalErrorThrowable) {
      context.response().setStatusCode(INTERNAL_SERVER_ERROR).end(t.getMessage());
    } else {
      context.fail(t);
    }
  }
}
