package vermillion.broker;

import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;
import org.apache.commons.lang3.math.NumberUtils;

public class BrokerVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(BrokerVerticle.class);

    @Override
    public void start(Promise<Void> promise) throws Exception {

        String brokerHost = config().getString("RABBITMQ_HOSTNAME");
        String brokerPortStr = config().getString("RABBITMQ_TCP_PORT");

        logger.debug("brokerportstr=" + brokerPortStr);
        logger.debug("port extract=" + brokerPortStr.split(":")[1]);
        int brokerPort;

        if (NumberUtils.isCreatable(brokerPortStr)) {
            // If the port number is 5672, 5671 etc.
            brokerPort = Integer.parseInt(brokerPortStr);
        } else {
            // If custom forwarding has been specified, or if forwarding is restricted to localhost
            // E.g. 127.0.0.1:5672
            brokerPort = Integer.parseInt(brokerPortStr.split(":")[1]);
        }

        logger.debug("port=" + brokerPort);
        String brokerUser = config().getString("RABBITMQ_USER");
        String brokerPass = config().getString("RABBITMQ_ADMIN_PASS");

        BrokerService.create(brokerHost, brokerPort, brokerUser, brokerPass, ready -> {
            if (ready.succeeded()) {
                ServiceBinder binder = new ServiceBinder(vertx.getDelegate());

                binder.setAddress("broker.queue").register(BrokerService.class, ready.result());

                promise.complete();

                logger.debug("Created broker service");
            } else {
                logger.debug("Could not create broker service. Cause =" + ready.cause());
                promise.fail(ready.cause());
            }
        });
    }
}
