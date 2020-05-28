package vermillion;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import vermillion.http.HttpServerVerticle;

public class MainVerticle extends AbstractVerticle {

  public static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> promise) throws Exception {
    int cpus = Runtime.getRuntime().availableProcessors();
    DeploymentOptions options = new DeploymentOptions().setInstances(cpus);

    vertx
        .rxDeployVerticle(HttpServerVerticle.class.getName(), options)
        .subscribe(id -> promise.complete(), promise::fail);

    logger.info("Deployed HTTP verticle");
  }
}
