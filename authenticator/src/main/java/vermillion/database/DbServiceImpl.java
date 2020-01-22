package vermillion.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.reactivex.pgclient.Row;
import io.reactivex.Flowable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.core.Vertx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class DbServiceImpl implements DbService {
    public PgPoolOptions options;
    public PgPool pool;
    public Vertx vertx;

    private final static Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);

    public DbServiceImpl(Vertx vertx, PgPoolOptions options, Handler<AsyncResult<DbService>> resultHandler) {
        this.vertx = vertx;
        this.options = options;
        pool = PgClient.pool(vertx, options);

        resultHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public DbService runQuery(String query, Handler<AsyncResult<List<String>>> resultHandler) {
        logger.debug("in run query");

        logger.debug("Query=" + query);

        pool.rxGetConnection()
                .map(conn -> {
                    conn
                            .rxQuery(query)
                            .flatMapPublisher(rows -> Flowable.fromIterable(rows))
                            .map(row -> row.toString())
                            .collect(ArrayList<String>::new, ArrayList::add)
                            .doAfterTerminate(conn::close)
                            .subscribe(SingleHelper.toObserver(resultHandler));
                    return null;
                })
                .subscribe();
        return this;
    }
}
