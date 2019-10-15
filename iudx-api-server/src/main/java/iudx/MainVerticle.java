package iudx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import iudx.broker.BrokerVerticle;
import iudx.database.DbVerticle;
import iudx.http.HttpServerVerticle;

public class MainVerticle extends AbstractVerticle
{	
	public	final static Logger logger = LoggerFactory.getLogger(MainVerticle.class);
	
	@Override
	public void start(Promise<Void> promise)throws Exception
	{	
		deployHelper(DbVerticle.class.getName())
		.setHandler(db -> {
			
			if(!db.succeeded())
			{
				logger.debug(db.cause());
				promise.fail(db.cause().toString());
			}
			
			deployHelper(BrokerVerticle.class.getName())
		.setHandler(broker -> {
			
			if(!broker.succeeded()) 
			{
				logger.debug(broker.cause());
				promise.fail(broker.cause().toString());
			}
			
			deployHelper(HttpServerVerticle.class.getName())
		.setHandler(http -> {
			
			if(!http.succeeded())
			{
				logger.debug(http.cause());
				promise.fail(http.cause().toString());
			}
			
			promise.complete();
		});
		});
		});
		
}
	private Future<Void> deployHelper(String name)
	{
		   Promise<Void> promise = Promise.promise();
		   
		   if("iudx.http.HttpServerVerticle".equals(name))
		   {
			   vertx.deployVerticle(name,	new DeploymentOptions()
							.setWorker(true)
					   		.setInstances(Runtime.getRuntime()
					   		.availableProcessors()*2), res -> {
			   if(res.succeeded()) 
			   {
				   logger.info("Deployed Verticle " + name);
				   promise.complete();
			   }
			   else
			   {
				   logger.fatal("Failed to deploy verticle " + res.cause());
				   promise.fail(res.cause());
			   }
					   						  
			});
		   }
		   else
		   {
			   vertx.deployVerticle(name, res -> 
			   {
			      if(res.failed())
			      {
			         logger.fatal("Failed to deploy verticle " + name + " Cause = "+res.cause());
			         promise.fail(res.cause());
			      } 
			      else 
			      {
			    	 logger.info("Deployed Verticle " + name);
			         promise.complete();
			      }
			   });
		   }
		   
		   return promise.future();
}
}	
