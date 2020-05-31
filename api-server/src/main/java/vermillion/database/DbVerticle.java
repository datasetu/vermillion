package vermillion.database;

import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class DbVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> promise) throws Exception {

    DbService.create(
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
