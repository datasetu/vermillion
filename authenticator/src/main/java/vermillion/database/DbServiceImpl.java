package vermillion.database;

import io.reactiverse.pgclient.PgPoolOptions;
import io.reactiverse.reactivex.pgclient.PgClient;
import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.Row;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import io.vertx.reactivex.core.Vertx;
import vermillion.throwables.InternalErrorThrowable;

import java.util.ArrayList;
import java.util.List;

public class DbServiceImpl implements DbService {
  private static final Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);
  public PgPoolOptions options;
  public PgPool pool;
  public Vertx vertx;

  public DbServiceImpl(
      Vertx vertx, PgPoolOptions options, Handler<AsyncResult<DbService>> resultHandler) {
    this.vertx = vertx;
    this.options = options;
    pool = PgClient.pool(vertx, options);

    resultHandler.handle(Future.succeededFuture(this));
  }

  @Override
  public DbService runSelectQuery(String query, Handler<AsyncResult<List<String>>> resultHandler) {
    logger.debug("in run select query");

    logger.debug("Query=" + query);

    pool.rxGetConnection()
        .flatMap(
            conn ->
                conn.rxQuery(query)
                    .flatMapPublisher(Flowable::fromIterable)
                    .map(Row::toString)
                    .collect(ArrayList<String>::new, ArrayList::add)
                    .doAfterTerminate(conn::close))
        .subscribe(SingleHelper.toObserver(resultHandler));

    return this;
  }

  @Override
  public DbService runQuery(String query, Handler<AsyncResult<Void>> resultHandler) {
    logger.debug("in run query");

    logger.debug("Query=" + query);

    pool.rxGetConnection()
        .flatMapCompletable(
            conn ->
                conn.rxQuery(query)
                    .flatMapPublisher(Flowable::fromIterable)
                    .map(Row::size)
                    .flatMapCompletable(
                        size ->
                            size == 0
                                ? Completable.complete()
                                : Completable.error(new InternalErrorThrowable("Query failed")))
                    .doAfterTerminate(conn::close))
        .subscribe(CompletableHelper.toObserver(resultHandler));

    return this;
  }
}
