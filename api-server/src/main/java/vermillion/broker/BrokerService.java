package vermillion.broker;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.GenIgnore;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@ProxyGen
@VertxGen
public interface BrokerService {
    @GenIgnore
    static BrokerService create(
            String brokerHost,
            int brokerPort,
            String brokerUser,
            String brokerPass,
            Handler<AsyncResult<BrokerService>> resultHandler) {
        return new BrokerServiceImpl(brokerHost, brokerPort, brokerUser, brokerPass, resultHandler);
    }

    @GenIgnore
    static vermillion.broker.reactivex.BrokerService createProxy(io.vertx.core.Vertx vertx, String address) {
        return new vermillion.broker.reactivex.BrokerService(new BrokerServiceVertxEBProxy(vertx, address));
    }

    @Fluent
    BrokerService publish(
            String token, String exchange, String routingKey, String message, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    BrokerService adminPublish(
            String exchange, String routingKey, String message, Handler<AsyncResult<Void>> resultHandler);
}
