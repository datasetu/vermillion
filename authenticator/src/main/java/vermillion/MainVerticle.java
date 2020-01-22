package vermillion;

import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import vermillion.database.DbVerticle;
import vermillion.http.HttpServerVerticle;

public class MainVerticle extends AbstractVerticle {

    public final static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> promise) throws Exception {
        int cpus = Runtime.getRuntime().availableProcessors();
        DeploymentOptions options = new DeploymentOptions().setInstances(cpus);

        vertx.rxDeployVerticle(DbVerticle.class.getName())
                .flatMap(id -> vertx.rxDeployVerticle(HttpServerVerticle.class.getName(), options))
                .subscribe(id -> promise.complete(), promise::fail);

        logger.info("Deployed all verticles");
    }
}	
