package vermillion;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import vermillion.broker.BrokerVerticle;
import vermillion.database.DbVerticle;
import vermillion.http.HttpServerVerticle;

public class MainVerticle extends AbstractVerticle {

    public static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> promise) {

        int cpus = Runtime.getRuntime().availableProcessors();

        ConfigRetriever retriever = ConfigRetriever.create(
                vertx, new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

        retriever
                .rxGetConfig()
                .flatMap(config -> vertx.rxDeployVerticle(
                                DbVerticle.class.getName(), new DeploymentOptions().setConfig(config))
                        .flatMap(id -> vertx.rxDeployVerticle(
                                HttpServerVerticle.class.getName(),
                                new DeploymentOptions().setInstances(cpus).setConfig(config)))
                        .flatMap(id -> vertx.rxDeployVerticle(
                                BrokerVerticle.class.getName(), new DeploymentOptions().setConfig(config))))
                .subscribe(id -> promise.complete(), promise::fail);

        logger.info("Deployed all verticles");
    }
}
