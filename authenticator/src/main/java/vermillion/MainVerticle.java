package vermillion;

import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.config.ConfigRetriever;
import io.vertx.reactivex.core.AbstractVerticle;
import vermillion.http.HttpServerVerticle;

public class MainVerticle extends AbstractVerticle {

  public static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> promise) {

    ConfigRetriever retriever =
        ConfigRetriever.create(
            vertx, new ConfigRetrieverOptions().addStore(new ConfigStoreOptions().setType("env")));

    int cpus = Runtime.getRuntime().availableProcessors();

    retriever
        .rxGetConfig()
        .flatMap(
            config -> {
              logger.debug(config.encodePrettily());
              return vertx.rxDeployVerticle(
                  HttpServerVerticle.class.getName(),
                  new DeploymentOptions().setInstances(cpus).setConfig(config));
            })
        .subscribe(id -> promise.complete(), promise::fail);

    logger.info("Deployed HTTP verticle");
  }
}
