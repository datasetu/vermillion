package vermillion.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ProxyGen
@VertxGen
public interface DbService {
  @GenIgnore
  static DbService create(
      String esHost, int esPort, String index, Handler<AsyncResult<DbService>> resultHandler) {
    return new DbServiceImpl(esHost, esPort, index, resultHandler);
  }

  @GenIgnore
  static vermillion.database.reactivex.DbService createProxy(
      io.vertx.core.Vertx vertx, String address) {
    return new vermillion.database.reactivex.DbService(new DbServiceVertxEBProxy(vertx, address));
  }

  @Fluent
  DbService runQuery(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler);
}
