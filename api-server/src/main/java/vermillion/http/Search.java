package vermillion.http;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.GenericValidator;
import vermillion.database.Queries;
import vermillion.http.HttpServerVerticle;
import vermillion.util.Utils;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.util.*;
import java.util.stream.Collector;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import org.joda.time.format.ISODateTimeFormat;

public class Search extends HttpServerVerticle {

    public final Logger logger = LoggerFactory.getLogger(Search.class);

    public final String READ_SCOPE = "read";

    public Utils utils = new Utils();

    public void search(RoutingContext context) {
        // TODO: Convert all types of responses to JSON

        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();
        JsonObject requestBody;

        // Init a variable to check if scrolling has been requested
        boolean scroll = false;

        Queries queries = new Queries();

        JsonObject geoQuery = queries.getGeoQuery();
        JsonObject termQuery = queries.getTermQuery();
        JsonObject termsQuery = queries.getTermsQuery();
        JsonArray filterQuery = queries.getFilterQuery();
        JsonObject baseQuery = queries.getBaseQuery();

        String resourceIDstr = null;
        JsonArray resourceIDArray = null;

        try {
            requestBody = context.getBodyAsJson();
        } catch (Exception e) {
            utils.apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
            return;
        }

        logger.debug("Body=" + requestBody.encode());

        if (!requestBody.containsKey("id")) {
            utils.apiFailure(context, new BadRequestThrowable("No id found in body"));
            return;
        }

        if (!requestBody.containsKey("geo_distance")
                && !requestBody.containsKey("time")
                && !requestBody.containsKey("attribute")) {
            utils.apiFailure(context, new BadRequestThrowable("Invalid request"));
            return;
        }

        Set<String> permittedFieldSet = new HashSet<>();
        permittedFieldSet.add("id");
        permittedFieldSet.add("token");
        permittedFieldSet.add("geo_distance");
        permittedFieldSet.add("time");
        permittedFieldSet.add("attribute");
        permittedFieldSet.add("size");
        permittedFieldSet.add("scroll_duration");

        if (!permittedFieldSet.containsAll(requestBody.fieldNames())) {
            utils.apiFailure(context, new BadRequestThrowable("Body contains unnecessary fields"));
            return;
        }

        Object resourceIdObj = requestBody.getValue("id");

        if (!(resourceIdObj instanceof String) && !(resourceIdObj instanceof JsonArray)) {
            utils.apiFailure(context, new BadRequestThrowable("Resource id is not valid"));
            return;
        }
        if (resourceIdObj instanceof JsonArray) {
            // Resource ID is an array of strings
            resourceIDArray = requestBody.getJsonArray("id");
            for (Object o : resourceIDArray) {
                if (!(o instanceof String)) {
                    utils.apiFailure(context, new BadRequestThrowable("Resource ID list should be a list of strings"));
                    return;
                }
                if ("".equalsIgnoreCase(o.toString())) {
                    utils.apiFailure(context, new BadRequestThrowable("Resource ID is empty"));
                    return;
                }
                if (!utils.isValidResourceID(o.toString())) {
                    utils.apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
                    return;
                }
                if (!(((String) o).endsWith(".public")) && !requestBody.containsKey("token")) {
                    utils.apiFailure(context, new BadRequestThrowable("No token found in request"));
                    return;
                }
            }
            termsQuery.getJsonObject("terms").put("id.keyword", resourceIDArray);
            filterQuery.add(termsQuery);
        } else {
            // Standalone resource ID
            resourceIDstr = requestBody.getString("id");
            if ("".equalsIgnoreCase(resourceIDstr)) {
                utils.apiFailure(context, new BadRequestThrowable("Resource ID is empty"));
                return;
            }
            if (!utils.isValidResourceID(resourceIDstr)) {
                utils.apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
                return;
            }
            if (!resourceIDstr.endsWith(".public") && !requestBody.containsKey("token")) {
                utils.apiFailure(context, new BadRequestThrowable("No token found in request"));
                return;
            }
            termQuery.getJsonObject("term").put("id.keyword", resourceIDstr);
            filterQuery.add(termQuery);
        }

        // Response size

        // Init default value of responses to 10k
        int size = 10000;

        if (requestBody.containsKey("size")) {
            Object sizeObj = requestBody.getValue("size");

            if (sizeObj instanceof String) {
                utils.apiFailure(context, new BadRequestThrowable("Response size should be an integer"));
                return;
            }

            try {
                size = NumberUtils.createInteger(sizeObj.toString());
            } catch (NumberFormatException numberFormatException) {
                utils.apiFailure(context, new BadRequestThrowable("Response size is not a valid integer"));
                return;
            }

            if (size <= 0 || size > 10000) {
                utils.apiFailure(context, new BadRequestThrowable("Response size must be between 1-10000"));
                return;
            }
        }

        // Geo Query
        if (requestBody.containsKey("geo_distance")) {
            Object geoDistanceObj = requestBody.getValue("geo_distance");

            if (!(geoDistanceObj instanceof JsonObject)) {
                utils.apiFailure(context, new BadRequestThrowable("Geo distance is not a valid Json Object"));
                return;
            }

            JsonObject geoDistance = requestBody.getJsonObject("geo_distance");

            logger.debug("geo distance=" + geoDistance.encodePrettily());

            if (!geoDistance.containsKey("coordinates") || !geoDistance.containsKey("distance")) {
                utils.apiFailure(
                        context, new BadRequestThrowable("Geo distance does not contain coordinates and/or distance"));
                return;
            }

            Object distanceObj = geoDistance.getValue("distance");

            if (!(distanceObj instanceof String)) {
                utils.apiFailure(context, new BadRequestThrowable("Distance is not a string"));
                return;
            }
            String distance = geoDistance.getString("distance");

            if (!distance.endsWith("m")) {
                utils.apiFailure(
                        context,
                        new BadRequestThrowable(
                                "Only metres are supported. Use the raw query interface for other units"));
                return;
            }

            String distanceQuantity = distance.substring(0, distance.length() - 1);

            logger.debug("Is a valid number ?" + NumberUtils.isCreatable(distanceQuantity));

            // If the number preceding m, km, cm etc is a valid number
            int geoDistanceQuantity;
            try{
                geoDistanceQuantity = Integer.parseInt(distanceQuantity);
                if(geoDistanceQuantity < 1){
                    utils.apiFailure(context, new BadRequestThrowable("Distance less than 1m"));
                    return;
                }
            }
            catch(NumberFormatException ex){
                utils.apiFailure(context, new BadRequestThrowable("Distance is not valid."));
                return;
            }
            Object coordinatesObj = geoDistance.getValue("coordinates");

            if (!(coordinatesObj instanceof JsonArray)) {
                utils.apiFailure(context, new BadRequestThrowable("Coordinates is not a valid JsonArray"));
                return;
            }

            JsonArray coordinates = geoDistance.getJsonArray("coordinates");
            logger.debug("coordinates=" + coordinates.encodePrettily());

            logger.debug("coordinates size = " + coordinates.size());

            if (coordinates.size() != 2) {
                utils.apiFailure(context, new BadRequestThrowable("Invalid coordinates"));
                return;
            }

            if((coordinates.getValue(0) instanceof String) || (coordinates.getValue(1) instanceof String))
            {
                utils.apiFailure(context, new BadRequestThrowable("Coordinates are not valid numbers"));
                return;
            }

            logger.debug("Coordinates lat check = "
                    + NumberUtils.isCreatable(coordinates.getValue(0).toString()));
            logger.debug("Coordinates lon check = "
                    + NumberUtils.isCreatable(coordinates.getValue(1).toString()));

            if (!NumberUtils.isCreatable(coordinates.getValue(0).toString())
                    || !NumberUtils.isCreatable(coordinates.getValue(1).toString())) {
                utils.apiFailure(context, new BadRequestThrowable("Coordinates should be valid numbers"));
                return;
            }

            double lat = coordinates.getDouble(0);
            double lon = coordinates.getDouble(1);

            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                utils.apiFailure(context, new BadRequestThrowable("Invalid coordinates"));
                return;
            }

            geoQuery.getJsonObject("geo_distance").put("distance", distance).put("coordinates", coordinates);

            filterQuery = queries.getFilterQuery().add(geoQuery);
        }

        // Timeseries queries
        if (requestBody.containsKey("time")) {

            Object timeObj = requestBody.getValue("time");

            if (!(timeObj instanceof JsonObject)) {
                utils.apiFailure(context, new BadRequestThrowable("Time is not a valid Json Object"));
                return;
            }

            JsonObject time = requestBody.getJsonObject("time");

            if (!time.containsKey("start") && !time.containsKey("end")) {
                utils.apiFailure(context, new BadRequestThrowable("Start and end fields missing"));
                return;
            }

            Object startObj = time.getValue("start");
            Object endObj = time.getValue("end");

            if (!(startObj instanceof String) || !(endObj instanceof String)) {
                utils.apiFailure(context, new BadRequestThrowable("Start and end objects are not strings"));
                return;
            }

            String start = time.getString("start");
            String end = time.getString("end");
//            Locale locale = new Locale("English", "IN");
//
//            if (!GenericValidator.isDate(start, locale) || !GenericValidator.isDate(end, locale)) {
//                utils.apiFailure(context, new BadRequestThrowable("Start and/or end strings are not valid dates"));
//                return;
//            }
//            /(\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z))|(\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d([+-][0-2]\d:[0-5]\d|Z))|(\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d([+-][0-2]\d:[0-5]\d|Z))/

            DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser();

            DateTime startDate;
            DateTime endDate;
            try
            {
                startDate = fmt.parseDateTime(start);
                endDate = fmt.parseDateTime(end);
                if(endDate.getMillis() - startDate.getMillis() < 0)
                {
                    utils.apiFailure(context, new BadRequestThrowable("End date is smaller than start date"));
                    return;
                }
            }
            catch (Exception ex){
                ex.printStackTrace();
                utils.apiFailure(context, new BadRequestThrowable("Start and/or end strings are not valid dates"));
                return;
            }

            JsonObject timeQuery = queries.getTimeQuery();
            timeQuery
                    .getJsonObject("range")
                    .getJsonObject("timestamp")
                    .put("gte", start)
                    .put("lte", end);

            filterQuery.add(timeQuery);
        }

        // Attribute query
        if (requestBody.containsKey("attribute")) {

            Object attributeObj = requestBody.getValue("attribute");

            if (!(attributeObj instanceof JsonObject)) {
                utils.apiFailure(context, new BadRequestThrowable("Attribute is not a valid Json Object"));
                return;
            }
            JsonObject attribute = requestBody.getJsonObject("attribute");
            JsonObject attributeQuery = new JsonObject();

            if (!attribute.containsKey("term")) {
                utils.apiFailure(context, new BadRequestThrowable("Attribute name is missing"));
                return;
            }

            Object attributeNameObj = attribute.getValue("term");

            if (!(attributeNameObj instanceof String)) {
                utils.apiFailure(context, new BadRequestThrowable("Term is not a string"));
                return;
            }

            String attributeName = attribute.getString("term").trim();

            if (attributeName == null || "".equals(attributeName)) {
                utils.apiFailure(context, new BadRequestThrowable("Term parameter is empty"));
                return;
            }

            if (!(attribute.containsKey("min") && attribute.containsKey("max"))
                    == !(attribute.containsKey("term") && attribute.containsKey("value"))) {

                utils.apiFailure(context, new BadRequestThrowable("Invalid attribute query"));
                return;
            }

            // TODO: Add a case where only min or max are provided. Not both
            // Case 1: When the attribute query is a number
            if (attribute.containsKey("min") && attribute.containsKey("max")) {

                Object minObj = attribute.getValue("min");
                Object maxObj = attribute.getValue("max");

                if (minObj instanceof String || maxObj instanceof String) {
                    utils.apiFailure(context, new BadRequestThrowable("Min and max values should be numbers"));
                    return;
                }

                if (!NumberUtils.isCreatable(minObj.toString()) || !NumberUtils.isCreatable(maxObj.toString())) {
                    utils.apiFailure(context, new BadRequestThrowable("Min and max values are not valid numbers"));
                    return;
                }

                Double min = attribute.getDouble("min");
                Double max = attribute.getDouble("max");

                if (min > max) {
                    utils.apiFailure(context, new BadRequestThrowable("Min value is greater than max"));
                    return;
                }
                attributeQuery = queries.getRangeQuery();

                attributeQuery
                        .getJsonObject("range")
                        .put(
                                "data." + attributeName,
                                new JsonObject().put("gte", min).put("lte", max));
                filterQuery.add(attributeQuery);

            } else {
                Object valueObj = attribute.getValue("value");
                if (!(valueObj instanceof String)) {
                    utils.apiFailure(context, new BadRequestThrowable("Value is not a valid string"));
                    return;
                }

                String value = attribute.getString("value");
                attributeQuery = new Queries().getTermQuery();
                attributeQuery.getJsonObject("term").put("data." + attributeName + ".keyword", value);
                filterQuery.add(attributeQuery);
            }
        }

        // Scroll feature

        String scrollStr = null;
        String scrollUnit;
        String scrollValueStr;
        int scrollValue;

        if (requestBody.containsKey("scroll_duration")) {
            scroll = true;

            Object scrollObj = requestBody.getValue("scroll_duration");

            if (!(scrollObj instanceof String)) {
                utils.apiFailure(context, new BadRequestThrowable("Scroll parameter must be a string"));
                return;
            }

            scrollStr = scrollObj.toString();
            if ("".equals(scrollStr) || scrollStr == null){
                utils.apiFailure(context, new BadRequestThrowable("Scroll parameter is empty"));
                return;
            }
            // If the value is 10m, separate out '10' and 'm'
            scrollUnit = scrollStr.substring(scrollStr.length() - 1);
            scrollValueStr = scrollStr.substring(0, scrollStr.length() - 1);

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
            logger.debug("Scroll value =" + scrollValue);
            logger.debug("Scroll unit =" + scrollUnit);
        }

        baseQuery.put("size", size).getJsonObject("query").getJsonObject("bool").put("filter", filterQuery);

        logger.debug(baseQuery.encodePrettily());

        // Trigger regular search function in three cases
        // 1. When the token provided is null: This is perfectly safe to do at this stage since secure
        // IDs are checked beforehand
        // 2. When token is provided but ID is a public ID
        // 3. When token is provided but ID is a list of public IDs
        // Don't know why anyone would do 2 & 3, but you never know
        if ((resourceIDstr != null && resourceIDstr.endsWith(".public"))
                || (resourceIDArray != null
                && resourceIDArray.stream().map(Object::toString).allMatch(s -> s.endsWith(".public")))) {
            if (scroll) {
                dbService
                        .rxSearch(baseQuery, true, scrollStr)
                        .subscribe(
                                result -> response.putHeader("content-type", "application/json")
                                        .end(result.encode()),
                                t -> utils.apiFailure(context, t));
            } else {
                dbService
                        .rxSearch(baseQuery, false, null)
                        .subscribe(
                                result -> response.putHeader("content-type", "application/json")
                                        .end(result.encode()),
                                t -> utils.apiFailure(context, t));
            }
        } else {
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
            JsonArray requestedIDs = new JsonArray();

            if (resourceIDstr != null) {
                requestedIDs.add(resourceIDstr);
            } else {
                // Get only the secure IDs from the list of all IDs provided
                requestedIDs = resourceIDArray.stream()
                        .map(Object::toString)
                        .filter(s -> !s.endsWith(".public"))
                        .collect(Collector.of(JsonArray::new, JsonArray::add, JsonArray::add));
                logger.debug("Requested IDs=" + requestedIDs.encodePrettily());
            }

            if (scroll) {
                String finalScrollStr = scrollStr;
                checkAuthorisation(token, READ_SCOPE, requestedIDs)
                        .andThen(Single.defer(() -> dbService.rxSecureSearch(baseQuery, token, true, finalScrollStr)))
                        .subscribe(
                                result -> response.putHeader("content-type", "application/json")
                                        .end(result.encode()),
                                t -> utils.apiFailure(context, t));
            } else {
                checkAuthorisation(token, READ_SCOPE, requestedIDs)
                        .andThen(Single.defer(() -> dbService.rxSecureSearch(baseQuery, token, false, null)))
                        .subscribe(
                                result -> response.putHeader("content-type", "application/json")
                                        .end(result.encode()),
                                t -> utils.apiFailure(context, t));
            }
        }
    }
}