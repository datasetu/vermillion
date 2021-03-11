package vermillion.database;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.CompletableHelper;
import io.vertx.reactivex.SingleHelper;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.InternalErrorThrowable;

import java.io.IOException;

public class DbServiceImpl implements DbService {

    private static final Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);

    private RestClient client;
    private String index;

    private String searchEndpoint;
    private String searchMethod;
    private Request searchRequest;

    private String insertEndpoint;
    private String insertMethod;
    private Request insertRequest;

    public DbServiceImpl(String esHost, int esPort, String index, Handler<AsyncResult<DbService>> resultHandler) {

        client = RestClient.builder(new HttpHost(esHost, esPort, "http")).build();
        this.index = index;
        this.searchEndpoint = "/" + this.index + "/_search";
        this.searchMethod = "GET";

        this.insertEndpoint = "/" + this.index + "/_doc";
        this.insertMethod = "POST";

        // TODO: Have a retry mechanism
        searchRequest = new Request(searchMethod, searchEndpoint);
        insertRequest = new Request(insertMethod, insertEndpoint);

        resultHandler.handle(Future.succeededFuture(this));
    }

    // TODO: Implement Scroll API
    @Override
    public DbService search(JsonObject query, Handler<AsyncResult<JsonArray>> resultHandler) {
        logger.debug("In regular search");
        logger.debug("Query=" + query.encode());

        searchRequest.setJsonEntity(query.encode());
        Response response = null;
        try {
            response = client.performRequest(searchRequest);
        } catch (IOException e) {
            logger.debug(e.getMessage());
            resultHandler.handle(
                    Future.failedFuture(
                            new InternalErrorThrowable(
                                    "Error while querying DB. Please check your inputs and try again. You will be rate-limited very soon if your subsequent queries trigger this error.")));
            return this;
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            resultHandler.handle(
                    Future.failedFuture(
                            new BadRequestThrowable(
                                    "Malformed query. Please check your inputs and try again. You will be rate-limited very soon if your subsequent queries trigger this error.")));
            return this;
        } else {
            JsonArray responseJson = null;
            try {
                responseJson = new JsonObject(EntityUtils.toString(response.getEntity()))
                        .getJsonObject("hits")
                        .getJsonArray("hits");
            } catch (IOException e) {
                e.printStackTrace();
            }

            JsonArray finalResponseJson = responseJson;
            Observable.create(observableEmitter -> {
                        // TODO: This might be expensive for large responses
                        for (int i = 0; i < finalResponseJson.size(); i++) {
                            observableEmitter.onNext(
                                    finalResponseJson.getJsonObject(i).getJsonObject("_source"));
                        }
                        observableEmitter.onComplete();
                    })
                    .collect(JsonArray::new, JsonArray::add)
                    .subscribe(SingleHelper.toObserver(resultHandler));
        }

        return this;
    }

    @Override
    public DbService secureSearch(JsonObject query, String token, Handler<AsyncResult<JsonArray>> resultHandler) {
        logger.debug("In secure search");
        logger.debug("Query=" + query.encode());

        String serverName = System.getenv("SERVER_NAME");

        searchRequest.setJsonEntity(query.encode());
        Response response = null;
        try {
            response = client.performRequest(searchRequest);
        } catch (IOException e) {
            logger.debug(e.getMessage());
            resultHandler.handle(
                    Future.failedFuture(
                            new InternalErrorThrowable(
                                    "Error while querying DB. Please check your inputs and try again. You will be rate-limited very soon if your subsequent queries trigger this error.")));
            return this;
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            resultHandler.handle(
                    Future.failedFuture(
                            new BadRequestThrowable(
                                    "Malformed query. Please check your inputs and try again. You will be rate-limited very soon if your subsequent queries trigger this error.")));
            return this;
        } else {
            JsonArray responseJson = null;
            try {
                responseJson = new JsonObject(EntityUtils.toString(response.getEntity()))
                        .getJsonObject("hits")
                        .getJsonArray("hits");
            } catch (IOException e) {
                e.printStackTrace();
            }

            JsonArray finalResponseJson = responseJson;
            Observable.create(observableEmitter -> {
                        // TODO: This might be expensive for large responses
                        for (int i = 0; i < finalResponseJson.size(); i++) {

                            JsonObject responsedocs =
                                    finalResponseJson.getJsonObject(i).getJsonObject("_source");

                            JsonObject responseData = responsedocs.getJsonObject("data");

                            if (responseData.containsKey("link")
                                    && "/download".equalsIgnoreCase(responseData.getString("link"))) {

                                String downloadLink = "https://"
                                        + serverName
                                        + "/download?token="
                                        + token
                                        + "&id="
                                        + responsedocs.getString("id");

                                responsedocs.getJsonObject("data").put("link", downloadLink);
                            }
                            observableEmitter.onNext(responsedocs);
                        }
                        observableEmitter.onComplete();
                    })
                    .collect(JsonArray::new, JsonArray::add)
                    .subscribe(SingleHelper.toObserver(resultHandler));

            return this;
        }
    }

    @Override
    public DbService insert(JsonObject query, Handler<AsyncResult<Void>> resultHandler) {
        // TODO: Don't insert into db directly use rabbitmq
        logger.debug("In insert query");
        logger.debug("Query=" + query.encode());

        Completable.fromCallable(() -> {
                    insertRequest.setJsonEntity(query.encode());
                    Response response = client.performRequest(insertRequest);

                    if (response.getStatusLine().getStatusCode() != 200)
                        return Completable.error(new InternalErrorThrowable("Errored while inserting"));

                    return Completable.complete();
                })
                .subscribe(CompletableHelper.toObserver(resultHandler));

        return this;
    }
}
