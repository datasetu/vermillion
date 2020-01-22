package vermillion.database;

import java.util.ArrayList;
import java.util.List;

import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.reactivex.pgclient.Row;
import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@ProxyGen
@VertxGen
public interface DbService 
{
	@Fluent
	DbService runQuery(String query, Handler<AsyncResult<List<String>>> resultHandler);
	
	@GenIgnore 
	static DbService create(io.vertx.reactivex.core.Vertx vertx,PgPoolOptions options, Handler<AsyncResult<DbService>> resultHandler)
	{
		return new DbServiceImpl(vertx, options, resultHandler);
	}
	
	@GenIgnore
	static vermillion.database.reactivex.DbService createProxy(io.vertx.core.Vertx vertx, String address)
	{
		return new vermillion.database.reactivex.DbService(new DbServiceVertxEBProxy(vertx, address));
	}
}
