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
import io.vertx.serviceproxy.ServiceException;
import org.apache.http.HttpHost;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import vermillion.throwables.InternalErrorThrowable;

import java.io.IOException;

public class DbServiceImpl implements DbService {

    private static final Logger logger = LoggerFactory.getLogger(DbServiceImpl.class);

    private RestClient client;
    private String index;

    private String searchEndpoint;
    private String searchMethod;
    private Request searchRequest;

    private String scrollEndpoint;
    private Request scrollRequest;
    private String scrollMethod;

    private String insertEndpoint;
    private String insertMethod;
    private Request insertRequest;

    public DbServiceImpl(String esHost, int esPort, String index, Handler<AsyncResult<DbService>> resultHandler) {

        client = RestClient.builder(new HttpHost(esHost, esPort, "http")).build();
        this.index = index;
        this.searchEndpoint = "/" + this.index + "/_search";
        this.searchMethod = "GET";

        this.scrollEndpoint = "/_search/scroll";
        this.scrollMethod = "POST";

        this.insertEndpoint = "/" + this.index + "/_doc";
        this.insertMethod = "POST";

        // TODO: Have a retry mechanism
        this.searchRequest = new Request(searchMethod, searchEndpoint);
        this.insertRequest = new Request(insertMethod, insertEndpoint);
        this.scrollRequest = new Request(scrollEndpoint, scrollMethod);

        resultHandler.handle(Future.succeededFuture(this));
    }

    // TODO: Implement Scroll API
    @Override
    public DbService search(
            JsonObject query, boolean scroll, String scrollDuration, Handler<AsyncResult<JsonObject>> resultHandler) {
        logger.debug("In regular search");
        logger.debug("Query=" + query.encode());

        if (scroll) {
            searchRequest = new Request(searchMethod, searchEndpoint + "?scroll=" + scrollDuration);
        }

        searchRequest.setJsonEntity(query.encode());
        Response response = null;
        try {
            response = client.performRequest(searchRequest);
        } catch (ResponseException e) {
            int responseCode = e.getResponse().getStatusLine().getStatusCode();

            if(responseCode == 400)
            {
                resultHandler.handle(ServiceException.fail(
                        400,
                        "Malformed inputs"));
                return this;
            } else {
                logger.debug(e.getMessage());
                resultHandler.handle(
                        ServiceException.fail(500, "Error while querying DB. Please check your inputs and try again."));
                return this;
            }
        } catch (Exception e) {
            resultHandler.handle(ServiceException.fail(500, "Internal Server Error"));
            return this;
        }

        JsonObject responseJson = null;
        try {
            responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));

        } catch (IOException e) {
            logger.debug(e.getMessage());
            resultHandler.handle(
                    ServiceException.fail(500, "Error while querying DB. Please check your inputs and try again."));
            return this;
        }

        JsonObject finalResponseJson = responseJson;

        Observable.create(observableEmitter -> {
                    JsonArray responseHits =
                            finalResponseJson.getJsonObject("hits").getJsonArray("hits");

                    // TODO: This might be expensive for large responses
                    for (int i = 0; i < responseHits.size(); i++) {
                        observableEmitter.onNext(responseHits.getJsonObject(i).getJsonObject("_source"));
                    }
                    observableEmitter.onComplete();
                })
                .collect(JsonArray::new, JsonArray::add)
                .map(hits -> {
                    JsonObject searchResponse = new JsonObject();

                    if (scroll) {
                        searchResponse.put("scroll_id", finalResponseJson.getString("_scroll_id"));
                    }

                    searchResponse.put("hits", hits);

                    return searchResponse;
                })
                .subscribe(SingleHelper.toObserver(resultHandler));

        return this;
    }

    @Override
    public DbService secureSearch(
            JsonObject query,
            String token,
            boolean scroll,
            String scrollDuration,
            Handler<AsyncResult<JsonObject>> resultHandler) {
        logger.debug("In secure search");
        logger.debug("Query=" + query.encode());

        String serverName = System.getenv("SERVER_NAME");

        if (scroll) {
            searchRequest = new Request(searchMethod, searchEndpoint + "?scroll=" + scrollDuration);
        }

        searchRequest.setJsonEntity(query.encode());
        Response response = null;
        try {
            response = client.performRequest(searchRequest);
        } catch (ResponseException e) {

            int responseCode = e.getResponse().getStatusLine().getStatusCode();

            if(responseCode == 400)
            {
                resultHandler.handle(ServiceException.fail(
                        400,
                        "Malformed inputs"));
                return this;
            } else {
                logger.debug("Error message = " + e.getMessage());
                resultHandler.handle(
                        ServiceException.fail(500, "Error while querying DB. Please check your inputs and try again."));
                return this;
            }
        } catch (Exception e) {
            logger.debug("Exception = " + e.getMessage());
            resultHandler.handle(ServiceException.fail(500, "Internal Server Error"));
            return this;
        }

        JsonObject responseJson = null;
        try {
            responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonObject finalResponseJson = responseJson;

        Observable.create(observableEmitter -> {
                    JsonArray responseHits =
                            finalResponseJson.getJsonObject("hits").getJsonArray("hits");

                    // TODO: This might be expensive for large responses
                    for (int i = 0; i < responseHits.size(); i++) {

                        JsonObject responsedocs = responseHits.getJsonObject(i).getJsonObject("_source");

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
                .map(hits -> {
                    JsonObject searchResponse = new JsonObject();

                    if (scroll) {
                        searchResponse.put("scroll_id", finalResponseJson.getString("_scroll_id"));
                    }

                    searchResponse.put("hits", hits);

                    return searchResponse;
                })
                .subscribe(SingleHelper.toObserver(resultHandler));

        return this;
    }

    @Override
    public DbService scrolledSearch(
            String scrollId,
            String scrollDuration,
            String token,
            JsonArray authorisedIDs,
            Handler<AsyncResult<JsonObject>> resultHandler) {

        logger.debug("In scrolled search");
        logger.debug("Scroll ID = " + scrollId);

        if (authorisedIDs != null) {
            logger.debug("Authorised IDs = " + authorisedIDs.encodePrettily());
        }

        String serverName = System.getenv("SERVER_NAME");

        JsonObject scrollRequestBody =
                new JsonObject().put("scroll_id", scrollId).put("scroll", scrollDuration);

        scrollRequest = new Request(scrollMethod, scrollEndpoint);
        scrollRequest.setJsonEntity(scrollRequestBody.encode());

        Response response = null;
        try {
            response = client.performRequest(scrollRequest);

        } catch (ResponseException e) {
            int statusCode = e.getResponse().getStatusLine().getStatusCode();

            if (statusCode == 404) {
                resultHandler.handle(
                        ServiceException.fail(404, "Reached end of pagination or incorrect/expired scroll ID"));
                return this;
            } else {
                resultHandler.handle(ServiceException.fail(
                        statusCode, "Error while querying DB. Please check your inputs and try again."));
                return this;
            }
        } catch (Exception e) {
            resultHandler.handle(ServiceException.fail(500, "Internal Server Error"));
            return this;
        }

        JsonObject responseJson = null;
        try {
            responseJson = new JsonObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        JsonObject finalResponseJson = responseJson;

        Observable.create(observableEmitter -> {
                    JsonArray responseHits =
                            finalResponseJson.getJsonObject("hits").getJsonArray("hits");

                    // TODO: This might be expensive for large responses
                    for (int i = 0; i < responseHits.size(); i++) {

                        JsonObject responseDoc = responseHits.getJsonObject(i).getJsonObject("_source");

                        String resourceID = responseDoc.getString("id");

                        // The data being accessed is a secure dataset
                        if (!resourceID.endsWith(".public")) {
                            if (token != null && authorisedIDs.contains(resourceID)) {
                                JsonObject responseData = responseDoc.getJsonObject("data");

                                if (responseData.containsKey("link")
                                        && "/download".equalsIgnoreCase(responseData.getString("link"))) {

                                    String downloadLink = "https://"
                                            + serverName
                                            + "/download?token="
                                            + token
                                            + "&id="
                                            + responseDoc.getString("id");

                                    responseDoc.getJsonObject("data").put("link", downloadLink);
                                }
                            } else {
                                continue;
                            }
                        }

                        observableEmitter.onNext(responseDoc);
                    }
                    observableEmitter.onComplete();
                })
                .collect(JsonArray::new, JsonArray::add)
                .map(hits -> new JsonObject()
                        .put("scroll_id", finalResponseJson.getString("_scroll_id"))
                        .put("hits", hits))
                .subscribe(SingleHelper.toObserver(resultHandler));

        return this;
    }

    // TODO: No longer used. Check occurrences and remove.
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