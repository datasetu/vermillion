package vermillion.http;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import vermillion.database.Queries;
import vermillion.util.Utils;
import vermillion.http.HttpServerVerticle;
import vermillion.database.reactivex.DbService;
import vermillion.throwables.BadRequestThrowable;

import java.util.*;

public class Latest extends HttpServerVerticle {

    public final Logger logger = LoggerFactory.getLogger(Latest.class);

    public final String READ_SCOPE = "read";

    public Utils utils = new Utils();

    // TODO: This can be a simple get request
    public void latest(RoutingContext context) {

        logger.debug("In latest API");
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String token = request.getParam("token");
        String resourceID = request.getParam("id");

        logger.debug("id=" + resourceID);
        logger.info("token=" + token);

        if (resourceID == null) {
            logger.debug("Resource id is null");
            utils.apiFailure(context, new BadRequestThrowable("No resource ID found in request"));
            return;
        }

        if (!utils.isValidResourceID(resourceID)) {
            logger.debug("Resource is is malformed");
            utils.apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
            return;
        }

        if (!resourceID.endsWith(".public") && token == null) {
            logger.debug("Id is secure but no access token provided");
            utils.apiFailure(context, new BadRequestThrowable("No access token found in request"));
            return;
        }

        if (token != null && !utils.isValidToken(token)) {
            logger.debug("Access token is malformed");
            utils.apiFailure(context, new BadRequestThrowable("Malformed access token"));
            return;
        }

        // Initialise queries object
        Queries queries = new Queries();

        JsonObject baseQuery = queries.getBaseQuery();
        JsonArray filterQuery = queries.getFilterQuery();
        JsonObject termQuery = queries.getTermQuery();

        termQuery.getJsonObject("term").put("id.keyword", resourceID);
        filterQuery.add(termQuery);
        baseQuery.getJsonObject("query").getJsonObject("bool").put("filter", filterQuery);

        JsonObject constructedQuery = queries.getLatestQuery(baseQuery);

        logger.debug("Latest query = " + constructedQuery.encodePrettily());

        if (resourceID.endsWith(".public")) {
            logger.debug("Search on public resources");
            dbService.rxSearch(constructedQuery, false, null).subscribe(result -> response.putHeader(
                            "content-type", "application/json")
                    .end(result.encode()));
        } else {
            logger.debug("Secure search");
            JsonArray requestedIDs = new JsonArray().add(resourceID);
            checkAuthorisation(token, READ_SCOPE, requestedIDs)
                    .andThen(Single.defer(() -> dbService.rxSecureSearch(constructedQuery, token, false, null)))
                    .subscribe(
                            result -> response.putHeader("content-type", "application/json")
                                    .end(result.encode()),
                            t -> utils.apiFailure(context, t));
        }
    }
}
