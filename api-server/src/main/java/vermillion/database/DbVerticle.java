package vermillion.database;

import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class DbVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> promise) {

    String esHost = config().getString("ES_HOSTNAME");
    String index = config().getString("ES_DEFAULT_INDEX");

    //    String esHost = "localhost";
    //    String index = "archive";

    /*ES's default port is 9200. No need to read from config file
     *since config file port specifies the port to which 9200 should be forwarded
     *and not the port to which ES should bind
     */
    int esPort = 9200;

    DbService.create(
        esHost,
        esPort,
        index,
        ready -> {
          if (ready.succeeded()) {
            ServiceBinder binder = new ServiceBinder(vertx.getDelegate());

            binder.setAddress("db.queue").register(DbService.class, ready.result());

            promise.complete();
          } else {
            promise.fail(ready.cause());
          }
        });
  }
}
