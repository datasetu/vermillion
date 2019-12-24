package vermillion.broker;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.serviceproxy.ServiceBinder;

public class BrokerVerticle extends AbstractVerticle
{
	private final static Logger logger = LoggerFactory.getLogger(BrokerVerticle.class);
	
	@Override
	public void start(Promise<Void> promise)throws Exception
	{	
		BrokerService.create(vertx, ready -> {
			
			if(ready.succeeded())
			{
				ServiceBinder binder = new ServiceBinder(vertx);
				
				binder.setAddress("broker.queue").register(BrokerService.class, ready.result());
				
				promise.complete();
				
				logger.debug("Created broker service");
			}
			else
			{
				logger.debug("Could not create broker service. Cause ="+ready.cause());
				promise.fail(ready.cause());
			}
			
		});
	}
}
