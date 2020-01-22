package vermillion.database;

import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import vermillion.URLs;

public class DbVerticle extends AbstractVerticle 
{
	public	final static Logger logger = LoggerFactory.getLogger(DbVerticle.class);

	@Override
	public void start(Promise<Void> promise) throws Exception
	{
		PgPoolOptions options = new PgPoolOptions();
        options.setDatabase(URLs.psql_database_name);
        options.setHost(URLs.psql_database_url); 
        options.setPort(URLs.psql_database_port);
        options.setUser(URLs.psql_database_username);
        options.setPassword(URLs.psql_database_password);
        options.setCachePreparedStatements(true);
        options.setMaxSize(10000);

        logger.debug("creating proxy");

        DbService.create(vertx,options, ready -> {
        	
        	if(ready.succeeded())
        	{
        		ServiceBinder binder = new ServiceBinder(vertx.getDelegate());
        		
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