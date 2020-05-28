package vermillion.broker;

import com.rabbitmq.client.*;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import vermillion.Utils;
import vermillion.database.DbServiceImpl;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class BrokerServiceImpl implements BrokerService {
  private static final Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);
  public Map<String, Channel> pool;
  public Connection connection;
  public Channel channel;
  public ConnectionFactory factory;
  public Map<String, Object> args;

  BrokerServiceImpl(Handler<AsyncResult<BrokerService>> result) {
    pool = new HashMap<String, Channel>();
    args = new HashMap<String, Object>();

    // args.put("x-max-length-bytes", 10485760); // 10Mib
    // args.put("x-message-ttl", 86400000); // 1 day
    args.put("x-queue-mode", "lazy"); // Queues are lazy

    factory = new ConnectionFactory();
    factory.setUsername("admin");
    factory.setPassword(Utils.getBrokerPassword());
    factory.setVirtualHost("/");
    factory.setHost(Utils.getBrokerUrl("admin"));
    factory.setPort(5672);
    factory.setAutomaticRecoveryEnabled(true);
    factory.setNetworkRecoveryInterval(10000);

    try {
      connection = factory.newConnection();
      channel = connection.createChannel();

      logger.debug("Rabbitmq channel created");

      pool.put("admin", channel);
      result.handle(Future.succeededFuture(this));
    } catch (Exception e) {
      e.printStackTrace();
      result.handle(Future.failedFuture(e.getCause()));
    }
  }

  public Channel getAdminChannel(String username) {
    String bucket_url = Utils.getBrokerUrl(username);
    String bucket_number = bucket_url.substring(6, bucket_url.length());

    logger.debug("bucket_url=" + bucket_url);
    logger.debug("bucket_number=" + bucket_number);

    String token = "admin" + ":" + bucket_number;

    if ((!pool.containsKey(token)) || (!pool.get(token).isOpen())) {
      factory = new ConnectionFactory();
      factory.setUsername("admin");
      factory.setPassword(Utils.getBrokerPassword());
      factory.setVirtualHost("/");
      factory.setHost(Utils.getBrokerUrl(username));
      factory.setPort(5672);

      try {
        connection = factory.newConnection();
        channel = connection.createChannel();

        logger.debug("Rabbitmq channel created");

        pool.put(token, channel);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return pool.get(token);
  }

  public Channel getAdminChannel(int node) {
    String token = "admin" + ":" + node;

    if ((!pool.containsKey(token)) || (!pool.get(token).isOpen())) {
      factory = new ConnectionFactory();
      factory.setUsername("admin");
      factory.setPassword(Utils.getBrokerPassword());
      factory.setVirtualHost("/");
      factory.setHost("rabbit" + String.valueOf(node));
      factory.setPort(5672);

      try {
        connection = factory.newConnection();
        channel = connection.createChannel();

        logger.debug("Rabbitmq channel created");

        pool.put(token, channel);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return pool.get(token);
  }

  public Channel getChannel(String pool_id) throws AuthenticationFailureException {
    if ((!pool.containsKey(pool_id)) || (!pool.get(pool_id).isOpen())) {
      String username = pool_id.split(":")[0];
      String password = pool_id.split(":")[1];

      factory = new ConnectionFactory();
      factory.setUsername(username);
      factory.setPassword(password);
      factory.setVirtualHost("/");
      factory.setHost(Utils.getBrokerUrl(username));
      factory.setPort(5672);

      try {
        connection = factory.newConnection();
        channel = connection.createChannel();

        logger.debug("Rabbitmq channel created");

        pool.put(pool_id, channel);

      } catch (AuthenticationFailureException ae) {
        throw new AuthenticationFailureException("ACCESS_REFUSED");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return pool.get(pool_id);
  }

  @Override
  public BrokerService createOwnerResources(String id, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("in create owner resources");

    Completable.fromCallable(
            () -> {
              try {
                getAdminChannel(id).exchangeDeclare(id + ".notification", "topic", true);
                getAdminChannel(id).queueDeclare(id + ".notification", true, false, false, args);

                return Completable.complete();
              } catch (Exception e) {

                return Completable.error(
                    new InternalErrorThrowable("Create owner resources failed"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService deleteOwnerResources(String id, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In delete owner resources");

    Completable.fromCallable(
            () -> {
              try {
                getAdminChannel(id).exchangeDelete(id + ".notification");
                getAdminChannel(id).queueDelete(id + ".notification");

                return Completable.complete();
              } catch (Exception e) {
                return Completable.error(
                    new InternalErrorThrowable("Could not delete owner resources"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService createOwnerBindings(String id, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In create owner bindings");

    Completable.fromCallable(
            () -> {
              try {
                getAdminChannel(id).queueBind(id + ".notification", id + ".notification", "#");
                getAdminChannel(id).queueBind("DATABASE", id + ".notification", "#");

                return Completable.complete();
              } catch (Exception e) {
                return Completable.error(
                    new InternalErrorThrowable("Could not create owner bindings"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService createEntityResources(String id, Handler<AsyncResult<Void>> resultHandler) {
    Completable.fromCallable(
            () -> {
              try {
                // create exchanges
                getAdminChannel(id).exchangeDeclare(id + ".public", "topic", true);
                getAdminChannel(id).exchangeDeclare(id + ".protected", "topic", true);
                getAdminChannel(id).exchangeDeclare(id + ".private", "topic", true);
                getAdminChannel(id).exchangeDeclare(id + ".notification", "topic", true);
                getAdminChannel(id).exchangeDeclare(id + ".publish", "topic", true);
                getAdminChannel(id).exchangeDeclare(id + ".diagnostics", "topic", true);

                // create queues
                getAdminChannel(id).queueDeclare(id, true, false, false, args);
                getAdminChannel(id).queueDeclare(id + ".private", true, false, false, args);
                getAdminChannel(id).queueDeclare(id + ".priority", true, false, false, args);
                getAdminChannel(id).queueDeclare(id + ".command", true, false, false, args);
                getAdminChannel(id).queueDeclare(id + ".notification", true, false, false, args);

                return Completable.complete();
              } catch (Exception e) {
                return Completable.error(
                    new InternalErrorThrowable("Could not create entity resources"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));
    return this;
  }

  @Override
  public BrokerService deleteEntityResources(
      JsonArray idList, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In delete entity resources");

    logger.debug("idList=" + idList.toString());

    Completable.fromCallable(
            () -> {
              if (idList.size() == 0) {
                return Completable.complete();
              } else {

                try {
                  // TODO: Do not use a plain loop. Use vertx.executeBlocking
                  for (int i = 0; i < idList.size(); i++) {
                    String id = idList.getString(i);
                    if ("".equals(id)) continue;

                    // delete exchanges
                    getAdminChannel(id).exchangeDelete(id + ".public");
                    getAdminChannel(id).exchangeDelete(id + ".protected");
                    getAdminChannel(id).exchangeDelete(id + ".private");
                    getAdminChannel(id).exchangeDelete(id + ".notification");
                    getAdminChannel(id).exchangeDelete(id + ".publish");
                    getAdminChannel(id).exchangeDelete(id + ".diagnostics");

                    // delete queues
                    getAdminChannel(id).queueDelete(id);
                    getAdminChannel(id).queueDelete(id + ".private");
                    getAdminChannel(id).queueDelete(id + ".priority");
                    getAdminChannel(id).queueDelete(id + ".command");
                    getAdminChannel(id).queueDelete(id + ".notification");
                  }
                  return Completable.complete();
                } catch (Exception e) {
                  return Completable.error(
                      new InternalErrorThrowable("Could not delete entity resources"));
                }
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));
    return this;
  }

  @Override
  public BrokerService createEntityBindings(String id, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In create entity bindings");

    Completable.fromCallable(
            () -> {
              try {
                getAdminChannel(id).queueBind(id + ".notification", id + ".notification", "#");
                getAdminChannel(id).queueBind(id + ".private", id + ".private", "#");

                getAdminChannel(id).queueBind("DATABASE", id + ".public", "#");
                getAdminChannel(id).queueBind("DATABASE", id + ".protected", "#");
                getAdminChannel(id).queueBind("DATABASE", id + ".private", "#");
                getAdminChannel(id).queueBind("DATABASE", id + ".notification", "#");
                getAdminChannel(id).queueBind("DATABASE", id + ".publish", "#");
                getAdminChannel(id).queueBind("DATABASE", id + ".diagnostics", "#");

                return Completable.complete();
              } catch (Exception e) {
                return Completable.error(
                    new InternalErrorThrowable("Could not create entity bindings"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));
    return this;
  }

  @Override
  public BrokerService bind(
      String queue, String exchange, String routingKey, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In bind");

    Completable.fromCallable(
            () -> {
              String source_bucket = Utils.getBrokerUrl(exchange);
              String dest_bucket = Utils.getBrokerUrl(queue);

              if (source_bucket.equals(dest_bucket) || Utils.single_node || Utils.clustered) {

                try {
                  getAdminChannel(exchange).queueBind(queue, exchange, routingKey);

                  return Completable.complete();
                } catch (Exception e) {
                  return Completable.error(new InternalErrorThrowable("Could not bind"));
                }

              } else {
                String shovel_name = source_bucket + ":" + dest_bucket + ":" + routingKey;
                String source_uri =
                    "amqp://admin:" + Utils.getBrokerPassword() + "@" + source_bucket + ":5672/%2f";
                String dest_uri =
                    "amqp://admin:" + Utils.getBrokerPassword() + "@" + dest_bucket + ":5672/%2f";
                String url_string =
                    "http://"
                        + source_bucket
                        + ":15672"
                        + "/api/parameters/shovel/%2f/"
                        + shovel_name
                        + "/";

                JsonObject body = new JsonObject();
                JsonObject value = new JsonObject();

                value.put("src-protocol", "amqp091");
                value.put("src-uri", source_uri);
                value.put("src-exchange", exchange);
                value.put("src-exchange-key", routingKey);
                value.put("dest-protocol", "amqp091");
                value.put("dest-uri", dest_uri);
                value.put("dest-queue", queue);

                body.put("value", value);

                String encoded =
                    Base64.getEncoder()
                        .encodeToString(
                            ("admin" + ":" + Utils.getBrokerPassword())
                                .getBytes(StandardCharsets.UTF_8));

                URL url;
                HttpURLConnection con;

                try {
                  url = new URL(url_string);
                  con = (HttpURLConnection) url.openConnection();
                  con.setRequestMethod("PUT");
                  con.setRequestProperty("Authorization", "Basic " + encoded);
                  con.setRequestProperty("Content-Type", "application/json; utf-8");
                  con.setDoOutput(true);

                  con.getOutputStream().write(body.encode().getBytes(StandardCharsets.UTF_8));

                  int code = con.getResponseCode();

                  logger.debug("response code=" + code);

                  if (code == HttpURLConnection.HTTP_OK) {
                    return Completable.complete();
                  } else {
                    String inputLine;
                    StringBuilder stringBuilder = new StringBuilder();
                    BufferedReader bufferedReader =
                        new BufferedReader(new InputStreamReader(con.getInputStream()));

                    while ((inputLine = bufferedReader.readLine()) != null) {
                      stringBuilder.append(inputLine);
                    }

                    logger.debug("Output message=" + stringBuilder.toString());

                    // TODO: Have better error messages
                    return Completable.error(new InternalErrorThrowable("Multi-node Bind failed"));
                  }
                } catch (Exception e) {
                  return Completable.error(new InternalErrorThrowable("Multi-node bind failed"));
                }
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));
    return this;
  }

  @Override
  public BrokerService adminPublish(
      String exchange,
      String routingKey,
      String message,
      Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In publish");

    Completable.fromCallable(
            () -> {
              try {
                // TODO: Set user-id
                getAdminChannel(exchange)
                    .basicPublish(exchange, routingKey, null, message.getBytes());

                logger.debug("Published message =" + message);
                return Completable.complete();
              } catch (Exception e) {
                logger.debug("Failed to publish message =" + message + " Cause=" + e.getMessage());
                return Completable.error(new InternalErrorThrowable("Admin publish failed"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService publish(
      String id,
      String apikey,
      String exchange,
      String routingKey,
      String message,
      Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In publish");

    Completable.fromCallable(
            () -> {
              try {
                // TODO: Set user-id
                getChannel(id + ":" + apikey)
                    .basicPublish(exchange, routingKey, null, message.getBytes());

                logger.debug("Published message =" + message);
                return Completable.complete();
              } catch (AuthenticationFailureException ae) {
                logger.debug("Authentication Exception");
                return Completable.error(new UnauthorisedThrowable("ACCESS_REFUSED"));
              } catch (Exception e) {
                logger.debug("Failed to publish message =" + message + " Cause=" + e);
                return Completable.error(new InternalErrorThrowable("Publish failed"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService unbind(
      String queue, String exchange, String routingKey, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In unbind");

    // TODO: Implement shovel delete logic here.

    String pool_id = "admin" + ":" + Utils.getBrokerPassword();

    Completable.fromCallable(
            () -> {
              try {
                getChannel(pool_id).queueUnbind(queue, exchange, routingKey);
                logger.debug("Unbind successful");
                return Completable.complete();
              } catch (Exception e) {
                logger.debug("Failed to unbind. Cause=" + e.getMessage());
                return Completable.error(new InternalErrorThrowable("Unbind failed"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));
    return this;
  }

  @Override
  public BrokerService subscribe(
      String id,
      String apikey,
      String message_type,
      int count,
      Handler<AsyncResult<JsonArray>> resultHandler) {

    Observable.create(
            observableEmitter -> {
              GetResponse resp;
              String pool_id = id + ":" + apikey;
              long start = System.currentTimeMillis();

              for (int i = 0; i < count; i++) {

                logger.debug("time difference = " + (System.currentTimeMillis() - start));
                JsonObject message = new JsonObject();

                try {
                  do {

                    String queue_name = id + message_type;

                    resp = getChannel(pool_id).basicGet(queue_name, true);

                    if (resp == null) continue;

                    message.put("body", new String(resp.getBody(), StandardCharsets.UTF_8));
                    message.put("from", resp.getEnvelope().getExchange());
                    message.put("subject", resp.getEnvelope().getRoutingKey());
                    message.put("sent-by", resp.getProps().getUserId());

                    observableEmitter.onNext(message);
                  } while ((resp == null) && ((System.currentTimeMillis() - start) <= 1000));
                } catch (Exception e) {
                  logger.debug(e.getMessage());
                  observableEmitter.onError(new InternalErrorThrowable("Subscribe failed"));
                }
              }
              observableEmitter.onComplete();
            })
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(SingleHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService createExchange(String exchange, Handler<AsyncResult<Void>> resultHandler) {
    Completable.fromCallable(
            () -> {
              try {
                getAdminChannel(exchange).exchangeDeclare(exchange, "topic", true);
                return Completable.complete();
              } catch (Exception e) {
                return Completable.error(new InternalErrorThrowable("Create exchange failed"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService createQueue(String queue, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("In create queue");

    Completable.fromCallable(
            () -> {
              try {
                if ("true".equals(System.getenv("SINGLE_NODE"))) {
                  getAdminChannel(queue).queueDeclare(queue, true, false, false, args);
                  return Completable.complete();
                } else {
                  int nodes = 0;

                  if (System.getenv("BROKERS_IN_SWARM") != null) {
                    nodes = Integer.parseInt(System.getenv("BROKERS_IN_SWARM"));

                    for (int i = 1; i <= nodes; i++) {
                      getAdminChannel(i).queueDeclare("DATABASE", true, false, false, args);
                    }

                  } else if (System.getenv("BROKERS_IN_CLUSTER") != null) {
                    nodes = Integer.parseInt(System.getenv("BROKERS_IN_CLUSTER"));
                    getAdminChannel(queue).queueDeclare(queue, true, false, false, args);
                  }
                  return Completable.complete();
                }
              } catch (Exception e) {
                return Completable.error(new InternalErrorThrowable("Create queue failed"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));
    return this;
  }

  @Override
  public BrokerService deleteExchange(String exchange, Handler<AsyncResult<Void>> resultHandler) {
    Completable.fromCallable(
            () -> {
              try {
                getAdminChannel(exchange).exchangeDelete(exchange);
                return Completable.complete();
              } catch (Exception e) {
                return Completable.error(new InternalErrorThrowable("Exchange delete failed"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public BrokerService deleteQueue(String queue, Handler<AsyncResult<Void>> resultHandler) {
    Completable.fromCallable(
            () -> {
              try {
                getAdminChannel(queue).queueDelete(queue);
                return Completable.complete();
              } catch (Exception e) {
                return Completable.error(new InternalErrorThrowable("Could not delete queue"));
              }
            })
        .subscribe(CompletableHelper.toObserver(resultHandler));
    return this;
  }
}
