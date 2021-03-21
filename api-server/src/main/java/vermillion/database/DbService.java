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
  DbService search(JsonObject query, boolean scroll, String scrollDuration, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DbService secureSearch(
      JsonObject query, String token, boolean scroll, String scrollDuration, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DbService scrolledSearch(String scrollId, String scrollDuration, String token, JsonArray authorisedIDs, Handler<AsyncResult<JsonObject>> resultHandler);

  @Fluent
  DbService insert(JsonObject query, Handler<AsyncResult<Void>> resultHandler);
}
