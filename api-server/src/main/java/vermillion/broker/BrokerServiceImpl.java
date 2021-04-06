package vermillion.broker;

import com.rabbitmq.client.AuthenticationFailureException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.reactivex.Completable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.CompletableHelper;
import vermillion.database.DbServiceImpl;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.util.HashMap;
import java.util.Map;

public class BrokerServiceImpl implements BrokerService {
    private static final Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);
    private Map<String, Channel> pool;
    private Connection connection;
    private Channel channel;
    private ConnectionFactory factory;
    private Map<String, Object> args;

    private String brokerHost;
    private int brokerPort;
    private String brokerUser;
    private String brokerPass;

    BrokerServiceImpl(
            String brokerHost,
            int brokerPort,
            String brokerUser,
            String brokerPass,
            Handler<AsyncResult<BrokerService>> result) {

    	  logger.debug("host="+brokerHost);
    	  logger.debug("port="+brokerPort);
    	  logger.debug("user="+brokerUser);
    	  logger.debug("pass="+brokerPass);

        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.brokerUser = brokerUser;
        this.brokerPass = brokerPass;

        this.pool = new HashMap<>();
        this.args = new HashMap<>();

        // args.put("x-max-length-bytes", 10485760); // 10Mib
        // args.put("x-message-ttl", 86400000); // 1 day
        this.args.put("x-queue-mode", "lazy"); // Queues are lazy

        this.factory = new ConnectionFactory();
        this.factory.setUsername(this.brokerUser);
        this.factory.setPassword(this.brokerPass);
        this.factory.setVirtualHost("/");
        this.factory.setHost(this.brokerHost);
        this.factory.setPort(this.brokerPort);
        this.factory.setAutomaticRecoveryEnabled(true);
        this.factory.setNetworkRecoveryInterval(10000);

        try {
            this.connection = this.factory.newConnection();
            this.channel = this.connection.createChannel();

            logger.debug("Rabbitmq channel created");

            this.pool.put(this.brokerUser, this.channel);
            result.handle(Future.succeededFuture(this));
        } catch (Exception e) {
            e.printStackTrace();
            result.handle(Future.failedFuture(e.getCause()));
        }
    }

    public Channel getAdminChannel() {

        String key = this.brokerUser + ":" + this.brokerPass;

        if ((!this.pool.containsKey(key)) || (!this.pool.get(key).isOpen())) {
            this.factory = new ConnectionFactory();
            this.factory.setUsername(this.brokerUser);
            this.factory.setPassword(this.brokerPass);
            this.factory.setVirtualHost("/");
            this.factory.setHost(this.brokerHost);
            this.factory.setPort(this.brokerPort);

            try {
                this.connection = this.factory.newConnection();
                this.channel = connection.createChannel();

                logger.debug("Rabbitmq channel created");

                this.pool.put(key, this.channel);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return this.pool.get(key);
    }

    public Channel getChannel(String token) throws AuthenticationFailureException {
        if ((!this.pool.containsKey(token)) || (!this.pool.get(token).isOpen())) {
            this.factory = new ConnectionFactory();
            this.factory.setUsername(token);
            this.factory.setPassword(null);
            this.factory.setVirtualHost("/");
            this.factory.setHost(this.brokerHost);
            this.factory.setPort(this.brokerPort);

            try {
                this.connection = this.factory.newConnection();
                this.channel = this.connection.createChannel();

                logger.debug("Rabbitmq channel created");

                this.pool.put(token, this.channel);

            } catch (AuthenticationFailureException ae) {
                throw new AuthenticationFailureException("ACCESS_REFUSED");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return pool.get(token);
    }

    @Override
    public BrokerService adminPublish(
            String exchange, String routingKey, String message, Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("In publish");

        Completable.fromCallable(() -> {
                    try {
                        // TODO: Set user-id
                        getAdminChannel().basicPublish(exchange, routingKey, null, message.getBytes());

                        logger.debug("Published message =" + message);
                        return Completable.complete();
                    } catch (Exception e) {
                        logger.debug("Failed to publish message =" + message + " Cause=" + e.getMessage());
                        return Completable.error(new InternalErrorThrowable("Admin publish failed"));
                    }
                })
                .subscribe(CompletableHelper.toObserver(resultHandler));

        return this;
    }

    @Override
    public BrokerService publish(
            String token,
            String exchange,
            String routingKey,
            String message,
            Handler<AsyncResult<Void>> resultHandler) {
        logger.debug("In publish");

        Completable.fromCallable(() -> {
                    try {
                        // TODO: Set user-id
                        getChannel(token).basicPublish(exchange, routingKey, null, message.getBytes());

                        logger.debug("Published message =" + message);
                        return Completable.complete();
                    } catch (AuthenticationFailureException ae) {
                        logger.debug("Authentication Exception");
                        return Completable.error(new UnauthorisedThrowable("ACCESS_REFUSED"));
                    } catch (Exception e) {
                        logger.debug("Failed to publish message =" + message + " Cause=" + e);
                        return Completable.error(new InternalErrorThrowable("Publish failed"));
                    }
                })
                .subscribe(CompletableHelper.toObserver(resultHandler));

        return this;
    }
}
