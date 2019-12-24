package vermillion.database;

import java.util.ArrayList;
import java.util.List;

import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgConnection;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.pgclient.PgRowSet;
import io.reactiverse.pgclient.Row;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DbServiceImpl implements DbService
{
	public	PgPoolOptions options;
	public	PgPool client;
	public	Vertx vertx;
	
	private final static Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);
	
	public DbServiceImpl(Vertx vertx, PgPoolOptions options, Handler<AsyncResult<DbService>> resultHandler) 
	{
		this.vertx		=	vertx;
		this.options	=	options;
		client			=	PgClient.pool(vertx, options);
		
		resultHandler.handle(Future.succeededFuture(this));
	}

	@Override
	public DbService runQuery(String query, Handler<AsyncResult<List<String>>> resultHandler) 
	{
		logger.debug("in run query");
		
		logger.debug("Query="+query);
		
		client.getConnection(connection -> {
			
			if(connection.succeeded())
			{
				logger.debug("got connection");
				
				PgConnection conn 	=  connection.result();
				
				conn.query(query, result -> {
					
					if(result.succeeded())
					{
						logger.debug("query succeeded");
						List<String> resultList	=	new ArrayList<>();
						PgRowSet rows			=	result.result();	
						
						for(Row row:rows)
						{
							resultList.add(row.toString());
						}
						
						logger.debug("ResultList="+resultList.toString());
						logger.debug("Row count="+rows.rowCount());
						
						conn.close();
						resultHandler.handle(Future.succeededFuture(resultList));
					}
					else
					{
						conn.close();
						logger.debug(result.cause());
						resultHandler.handle(Future.failedFuture(result.cause()));
					}
				});
			}
		});
		
		return this;
	}
}
