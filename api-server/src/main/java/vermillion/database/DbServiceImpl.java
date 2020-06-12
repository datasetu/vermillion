package vermillion.database;

import io.reactivex.Observable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.SingleHelper;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;

public class DbServiceImpl implements DbService {

  private static final Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);
  RestClient client;
  String index;
  String endpoint;
  String method;
  Request request;

  public DbServiceImpl(
      String esHost, int esPort, String index, Handler<AsyncResult<DbService>> resultHandler) {

    client = RestClient.builder(new HttpHost(esHost, esPort, "http")).build();
    this.index = index;
    this.endpoint = "/" + this.index + "/_search";
    this.method = "GET";
    //TODO: Have a retry mechanism
    request = new Request(method, endpoint);
    resultHandler.handle(Future.succeededFuture(this));
  }

  // TODO: Implement Scroll API
  @Override
  public DbService runQuery(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler) {
    logger.debug("In run query");

    logger.debug("Query=" + query.encode());

    Observable.create(
        observableEmitter -> {
          request.setJsonEntity(query.encode());
          Response response = client.performRequest(request);

          JsonArray responseJson =
              new JsonObject(EntityUtils.toString(response.getEntity()))
                  .getJsonObject("hits")
                  .getJsonArray("hits");

          //TODO: This might be expensive for large responses
          for (int i = 0; i < responseJson.size(); i++) {
            observableEmitter.onNext(responseJson.getJsonObject(i).getJsonObject("_source"));
          }
          observableEmitter.onComplete();
        })
        .collect(JsonArray::new, JsonArray::add)
        .subscribe(SingleHelper.toObserver(resultHandler));

    return this;
  }
}
