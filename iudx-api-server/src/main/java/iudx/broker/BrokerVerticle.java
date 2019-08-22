package iudx.broker;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.serviceproxy.ServiceBinder;

public class BrokerVerticle extends AbstractVerticle
{
	@Override
	public void start(Future<Void> startFuture)
	{	
		BrokerService.create(vertx, ready -> {
			
			if(ready.succeeded())
			{
				ServiceBinder binder = new ServiceBinder(vertx);
				
				binder.setAddress("broker.queue").register(BrokerService.class, ready.result());
				
				startFuture.complete();
			}
			else
			{
				startFuture.fail(ready.cause());
			}
			
		});
	}
}
