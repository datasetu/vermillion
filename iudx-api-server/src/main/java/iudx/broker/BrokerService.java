package iudx.broker;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;

@ProxyGen
@VertxGen
public interface BrokerService 
{
	@Fluent
	BrokerService createOwnerResources(String id, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService deleteOwnerResources(String id, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService createOwnerBindings(String id, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService createEntityResources(String id, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService deleteEntityResources(String id_list, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService createEntityBindings(String id, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService bind(String queue, String exchange, String routingKey, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService unbind(String queue, String exchange, String routingKey, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService publish(String id, String apikey, String exchange, String routingKey, String message, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService subscribe(String id, String apikey, String message_type, int count, Handler<AsyncResult<JsonArray>> resultHandler);
	
	@Fluent
	BrokerService adminPublish(String exchange, String routingKey, String message, Handler<AsyncResult<Void>> resultHandler);
	
	@Fluent
	BrokerService createExchange(String exchange, Handler<AsyncResult<JsonArray>> resultHandler);
	
	@Fluent
	BrokerService createQueue(String queue, Handler<AsyncResult<JsonArray>> resultHandler);
	
	@Fluent
	BrokerService deleteExchange(String exchange, Handler<AsyncResult<JsonArray>> resultHandler);
	
	@Fluent
	BrokerService deleteQueue(String queue, Handler<AsyncResult<JsonArray>> resultHandler);
	
	@GenIgnore
	static BrokerService create(Vertx vertx, Handler<AsyncResult<BrokerService>> resultHandler)
	{
		return new BrokerServiceImpl(vertx, resultHandler);
	}
	
	@GenIgnore
	static BrokerService createProxy(Vertx vertx, String address)
	{
		return new BrokerServiceVertxEBProxy(vertx, address);
	}
	
}
