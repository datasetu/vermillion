package vermillion.http;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.GenericValidator;
import vermillion.database.Queries;
import vermillion.util.Utils;
import vermillion.http.HttpServerVerticle;
import vermillion.database.reactivex.DbService;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.util.*;

public class ScrolledSearch extends HttpServerVerticle {

    public final Logger logger = LoggerFactory.getLogger(ScrolledSearch.class);

    public final String READ_SCOPE = "read";

    public Utils utils = new Utils();

    // TODO: Should the scroll API need special permissions?
    // After all, it puts quite a bit of load on the server for large responses
    public void scrolledSearch(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        JsonObject requestBody;
        int scrollValue;

        try {
            requestBody = context.getBodyAsJson();

            if (requestBody == null) {
                utils.apiFailure(context, new BadRequestThrowable("Body is empty"));
                return;
            }

        } catch (Exception e) {
            utils.apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
            return;
        }

        logger.debug("Body=" + requestBody.encode());

        if (!requestBody.containsKey("scroll_id")) {
            utils.apiFailure(context, new BadRequestThrowable("Obtain a scroll_id from the search API first"));
            return;
        }

        if (!requestBody.containsKey("scroll_duration")) {
            utils.apiFailure(context, new BadRequestThrowable("Scroll duration not specified"));
            return;
        }

        Set<String> permittedFieldSet = new HashSet<>();
        permittedFieldSet.add("scroll_id");
        permittedFieldSet.add("token");
        permittedFieldSet.add("scroll_duration");

        if (!permittedFieldSet.containsAll(requestBody.fieldNames())) {
            utils.apiFailure(context, new BadRequestThrowable("Body contains unnecessary fields"));
            return;
        }

        Object scrollIdObj = requestBody.getValue("scroll_id");

        if (!(scrollIdObj instanceof String))
        {
            utils.apiFailure(context, new BadRequestThrowable("Scroll ID is not valid"));
            return;
        }

        Object scrollDurationObj = requestBody.getValue("scroll_duration");

        if (!(scrollDurationObj instanceof String))
        {
            utils.apiFailure(context, new BadRequestThrowable("Scroll Duration is not valid"));
            return;
        }

        String scrollId = requestBody.getString("scroll_id");

        String scrollDuration = requestBody.getString("scroll_duration");

        if("".equals(scrollId) || scrollId == null){
            utils.apiFailure(context, new BadRequestThrowable("Scroll Id is empty"));
            return;
        }

//        if(!isValidScrollID(scrollId)){
//            utils.apiFailure(context, new BadRequestThrowable("Invalid Scroll Id"));
//            return;
//        }

        if("".equals(scrollDuration) || scrollDuration == null){
            utils.apiFailure(context, new BadRequestThrowable("Scroll Duration is empty"));
            return;
        }

        String scrollUnit = scrollDuration.substring(scrollDuration.length() - 1);
        String scrollValueStr = scrollDuration.substring(0, scrollDuration.length() - 1);

        try {
            scrollValue = NumberUtils.createInteger(scrollValueStr);
        } catch (NumberFormatException numberFormatException) {
            utils.apiFailure(context, new BadRequestThrowable("Scroll value is not a valid integer"));
            return;
        }

        if (scrollValue <= 0) {
            utils.apiFailure(context, new BadRequestThrowable("Scroll value cannot be less than or equal to zero"));
            return;
        }

        if ((scrollUnit.equalsIgnoreCase("h") && scrollValue != 1)
                || (scrollUnit.equals("m") && scrollValue > 60)
                || (scrollUnit.equalsIgnoreCase("s") && scrollValue > 3600)) {
            utils.apiFailure(
                    context,
                    new BadRequestThrowable(
                            "Scroll value is too large. Max scroll duration cannot be more than 1 hour"));
            return;
        }
        else if (!scrollUnit.equalsIgnoreCase("h") && !scrollUnit.equals("m") && !scrollUnit.equalsIgnoreCase("s")) {
            utils.apiFailure(
                    context,
                    new BadRequestThrowable(
                            "Scroll unit is invalid"));
            return;
        }

        if (requestBody.containsKey("token")) {
            Object tokenObj = requestBody.getValue("token");

            if (!(tokenObj instanceof String)) {
                utils.apiFailure(context, new BadRequestThrowable("token is not valid"));
                return;
            }

            String token = requestBody.getString("token");

            if("".equals(token) || token == null){
                utils.apiFailure(context, new BadRequestThrowable("token is empty"));
                return;
            }

            if (token != null && !utils.isValidToken(token)) {
                utils.apiFailure(context, new UnauthorisedThrowable("Invalid Token"));
                return;
            }

            checkAuthorisation(token, READ_SCOPE)
                    .flatMap(
                            authorisedIDs -> dbService.rxScrolledSearch(scrollId, scrollDuration, token, authorisedIDs))
                    .subscribe(
                            result -> response.putHeader("content-type", "application/json")
                                    .end(result.encode()),
                            t -> utils.apiFailure(context, t));
        } else {
            dbService
                    .rxScrolledSearch(scrollId, scrollDuration, null, null)
                    .subscribe(
                            result -> response.putHeader("content-type", "application/json")
                                    .end(result.encode()),
                            t -> utils.apiFailure(context, t));
        }
    }
}