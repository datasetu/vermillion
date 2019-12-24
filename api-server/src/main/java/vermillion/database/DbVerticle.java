package vermillion.database;

import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.serviceproxy.ServiceBinder;
import vermillion.Utils;

public class DbVerticle extends AbstractVerticle 
{
	public PgPool client;

	@Override
	public void start(Promise<Void> promise) throws Exception
	{
		PgPoolOptions options = new PgPoolOptions();
        options.setDatabase(Utils.psql_database_name);
        options.setHost(Utils.psql_database_url); 
        options.setPort(Utils.psql_database_port);
        options.setUser(Utils.psql_database_username);
        options.setPassword(Utils.psql_database_password);
        options.setCachePreparedStatements(true);
        options.setMaxSize(10000);
		
        DbService.create(vertx, options, ready -> {
        	
        	if(ready.succeeded())
        	{
        		ServiceBinder binder = new ServiceBinder(vertx);
        		
        		binder.setAddress("db.queue").register(DbService.class, ready.result());
        		
        		promise.complete();
        	}
        	else
        	{
        		promise.fail(ready.cause());
        	}
        });
	}	
}