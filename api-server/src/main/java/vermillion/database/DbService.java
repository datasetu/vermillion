package vermillion.database;

import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

@ProxyGen
@VertxGen
public interface DbService {
  @GenIgnore
  static DbService create(
      io.vertx.reactivex.core.Vertx vertx,
      PgPoolOptions options,
      Handler<AsyncResult<DbService>> resultHandler) {
    return new DbServiceImpl(vertx, options, resultHandler);
  }

  @GenIgnore
  static vermillion.database.reactivex.DbService createProxy(
      io.vertx.core.Vertx vertx, String address) {
    return new vermillion.database.reactivex.DbService(new DbServiceVertxEBProxy(vertx, address));
  }

  @Fluent
  DbService runSelectQuery(String query, Handler<AsyncResult<List<String>>> resultHandler);

  @Fluent
  DbService runQuery(String query, Handler<AsyncResult<Void>> resultHandler);
}
