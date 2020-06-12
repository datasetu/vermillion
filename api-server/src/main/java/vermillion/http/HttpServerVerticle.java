package vermillion.http;

import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import java.util.Locale;
import org.apache.commons.lang3.math.NumberUtils;
import vermillion.database.Queries;
import vermillion.database.reactivex.DbService;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.ConflictThrowable;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;
import org.apache.commons.validator.GenericValidator;

public class HttpServerVerticle extends AbstractVerticle {

  public final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);
  // HTTP Codes
  public final int OK = 200;
  public final int CREATED = 201;
  public final int ACCEPTED = 202;
  public final int BAD_REQUEST = 400;
  public final int FORBIDDEN = 403;
  public final int CONFLICT = 409;
  public final int INTERNAL_SERVER_ERROR = 500;

  // Service Proxies
  public DbService dbService;

  @Override
  public void start(Promise<Void> startPromise) {
    logger.debug("In start");

    int port = 443;

    dbService = vermillion.database.DbService.createProxy(vertx.getDelegate(), "db.queue");

    Router router = Router.router(vertx);

    router.route().handler(BodyHandler.create());

    router.post("/latest").handler(this::latest);
    router.post("/search").handler(this::search);

    vertx
        .createHttpServer(
            new HttpServerOptions()
                .setSsl(true)
                .setCompressionSupported(true)
                .setKeyStoreOptions(
                    new JksOptions().setPath("my-keystore.jks").setPassword("password")))
        .requestHandler(router)
        .rxListen(port)
        .subscribe(
            s -> {
              logger.debug("Server started");
              startPromise.complete();
            },
            err -> {
              logger.debug("Could not start server. Cause=" + err.getMessage());
              startPromise.fail(err.getMessage());
            });
  }

  public void latest(RoutingContext context) {
    logger.debug("In latest API");
    HttpServerResponse response = context.response();

    JsonObject requestBody;

    try {
      requestBody = context.getBodyAsJson();
    } catch (Exception e) {
      apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
      return;
    }

    logger.debug("Body=" + requestBody.encode());

    if (!requestBody.containsKey("id")) {
      apiFailure(context, new BadRequestThrowable("No id found in body"));
      return;
    }

    String resourceID = requestBody.getString("id");

    // Intitialise queries object
    Queries queries = new Queries();

    JsonObject baseQuery = queries.getBaseQuery();
    JsonArray filterQuery = queries.getFilterQuery();
    JsonObject termQuery = queries.getTermQuery();

    termQuery.getJsonObject("term").put("id.keyword", resourceID);
    filterQuery.add(termQuery);
    baseQuery.getJsonObject("query").getJsonObject("bool").put("filter", filterQuery);

    JsonObject constructedQuery = queries.getLatestQuery(baseQuery);

    logger.debug(constructedQuery.encodePrettily());

    dbService
        .rxRunQuery(constructedQuery)
        .subscribe(
            result -> {
              response.putHeader("content-type", "application/json").end(result.encode());
            });
  }

  public void search(RoutingContext context) {
    // TODO: Convert all types of responses to JSON

    HttpServerRequest request = context.request();
    HttpServerResponse response = context.response();

    JsonObject requestBody;

    try {
      requestBody = context.getBodyAsJson();
    } catch (Exception e) {
      apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
      return;
    }

    logger.debug("Body=" + requestBody.encode());

    if (!requestBody.containsKey("id")) {
      apiFailure(context, new BadRequestThrowable("No id found in body"));
      return;
    }

    if (!requestBody.containsKey("geo_distance") && !requestBody.containsKey("time")) {
      apiFailure(context, new BadRequestThrowable("Invalid request"));
      return;
    }

    Object resourceIdObj = requestBody.getValue("id");

    if (!(resourceIdObj instanceof String)) {
      apiFailure(context, new BadRequestThrowable("Resource id is not a valid string"));
      return;
    }
    String resourceID = requestBody.getString("id");

    Queries queries = new Queries();

    JsonObject geoQuery = queries.getGeoQuery();
    JsonObject termQuery = queries.getTermQuery();
    JsonArray filterQuery = queries.getFilterQuery();
    JsonObject baseQuery = queries.getBaseQuery();

    termQuery.getJsonObject("term").put("id.keyword", resourceID);

    filterQuery.add(termQuery);

    // Geo Query
    if (requestBody.containsKey("geo_distance")) {
      Object geoDistanceObj = requestBody.getValue("geo_distance");

      if (!(geoDistanceObj instanceof JsonObject)) {
        apiFailure(context, new BadRequestThrowable("Geo distance is not a valid Json Object"));
        return;
      }

      JsonObject geoDistance = requestBody.getJsonObject("geo_distance");

      logger.debug("geo distance=" + geoDistance.encodePrettily());

      if (!geoDistance.containsKey("coordinates") || !geoDistance.containsKey("distance")) {
        apiFailure(
            context,
            new BadRequestThrowable("Geo distance does not contain coordinates and/or distance"));
        return;
      }

      Object distanceObj = geoDistance.getValue("distance");

      if (!(distanceObj instanceof String)) {
        apiFailure(context, new BadRequestThrowable("Distance is not a string"));
        return;
      }
      String distance = geoDistance.getString("distance");

      if (!distance.endsWith("m") || !distance.endsWith("km")) {
        apiFailure(
            context,
            new BadRequestThrowable(
                "Only metre and kilometre units supported. Use raw query interface for other units"));
        return;
      }

      logger.debug(NumberUtils.isCreatable(distance.substring(0, distance.length() - 2)));

      // If the number preceding m, km, cm etc is a valid number
      if (!NumberUtils.isCreatable(distance.substring(0, distance.length() - 2))) {
        apiFailure(context, new BadRequestThrowable("Distance is not a valid number"));
        return;
      }

      Object coordinatesObj = geoDistance.getValue("coordinates");

      if (!(coordinatesObj instanceof JsonArray)) {
        apiFailure(context, new BadRequestThrowable("Coordinates is not a valid JsonArray"));
        return;
      }

      JsonArray coordinates = geoDistance.getJsonArray("coordinates");
      logger.debug("coordinates=" + coordinates.encodePrettily());

      logger.debug("coordinates size = " + coordinates.size());

      if (coordinates.size() != 2) {
        apiFailure(context, new BadRequestThrowable("Invalid coordinates"));
        return;
      }

      logger.debug(
          "Coordinates lat check = " + NumberUtils.isCreatable(coordinates.getValue(0).toString()));
      logger.debug(
          "Coordinates lon check = " + NumberUtils.isCreatable(coordinates.getValue(0).toString()));

      if (!NumberUtils.isCreatable(coordinates.getValue(0).toString())
          || !NumberUtils.isCreatable(coordinates.getValue(1).toString())) {
        apiFailure(context, new BadRequestThrowable("Invalid coordinates"));
        return;
      }

      geoQuery
          .getJsonObject("geo_distance")
          .put("distance", distance)
          .put("coordinates", coordinates);

      filterQuery = queries.getFilterQuery().add(geoQuery);
    }

    // Timeseries queries
    if (requestBody.containsKey("time")) {

      Object timeObj = requestBody.getValue("time");

      if (!(timeObj instanceof JsonObject)) {
        apiFailure(context, new BadRequestThrowable("Time is not a valid Json Object"));
        return;
      }

      JsonObject time = requestBody.getJsonObject("time");

      if (!time.containsKey("start") && !time.containsKey("end")) {
        apiFailure(context, new BadRequestThrowable("Start and end fields missing"));
        return;
      }

      Object startObj = time.getValue("start");
      Object endObj = time.getValue("end");

      if (!(startObj instanceof String) && !(endObj instanceof String)) {
        apiFailure(context, new BadRequestThrowable("Start and end objects are not strings"));
        return;
      }

      String start = time.getString("start");
      String end = time.getString("end");
      Locale locale = new Locale("English", "UK");

      if (!GenericValidator.isDate(start, locale) || !GenericValidator.isDate(end, locale)) {
        apiFailure(
            context, new BadRequestThrowable("Start and/or end strings are not valid dates"));
        return;
      }
      JsonObject timeQuery = queries.getTimeQuery();
      timeQuery.getJsonObject("range").getJsonObject("timestamp").put("gte", start).put("lte", end);
      filterQuery.add(timeQuery);
    }

    // Attribute query
    if (requestBody.containsKey("attribute")) {

      Object attributeObj = requestBody.getValue("attribute");

      if (!(attributeObj instanceof JsonObject)) {
        apiFailure(context, new BadRequestThrowable("Attribute is not a valid Json Object"));
        return;
      }
      JsonObject attribute = requestBody.getJsonObject("attribute");
      JsonObject attributeQuery = new JsonObject();

      if (!attribute.containsKey("term")) {
        apiFailure(context, new BadRequestThrowable("Attribute name is missing"));
        return;
      }

      Object attributeNameObj = attribute.getValue("term");

      if (!(attributeNameObj instanceof String)) {
        apiFailure(context, new BadRequestThrowable("Term is not a string"));
        return;
      }

      String attributeName = attribute.getString("term");

      if (!(attribute.containsKey("min") && attribute.containsKey("max"))
          == !(attribute.containsKey("term") && attribute.containsKey("value"))) {

        apiFailure(context, new BadRequestThrowable("Invalid attribute query"));
        return;
      }

      // Case 1: When the attribute query is a number
      if (attribute.containsKey("min") && attribute.containsKey("max")) {

        Object minObj = attribute.getValue("min");
        Object maxObj = attribute.getValue("max");

        if (!NumberUtils.isCreatable(minObj.toString())
            || !NumberUtils.isCreatable(maxObj.toString())) {
          apiFailure(context, new BadRequestThrowable("Min and max values are not valid numbers"));
          return;
        }

        Double min = attribute.getDouble("min");
        Double max = attribute.getDouble("max");

        attributeQuery = queries.getRangeQuery();

        attributeQuery
            .getJsonObject("range")
            .put("data." + attributeName, new JsonObject().put("gte", min).put("lte", max));
        filterQuery.add(attributeQuery);

      } else {
        Object valueObj = attribute.getValue("value");
        if (!(valueObj instanceof String)) {
          apiFailure(context, new BadRequestThrowable("Value is not a valid string"));
          return;
        }

        String value = attribute.getString("value");
        attributeQuery = new Queries().getTermQuery();
        attributeQuery.getJsonObject("term").put("data." + attributeName + ".keyword", value);
        filterQuery.add(attributeQuery);
      }
    }

    baseQuery.getJsonObject("query").getJsonObject("bool").put("filter", filterQuery);

    logger.debug(baseQuery.encodePrettily());

    dbService
        .rxRunQuery(baseQuery)
        .subscribe(
            result -> response.putHeader("content-type", "application/json").end(result.encode()));
  }

  public boolean isStringSafe(String resource) {
    logger.debug("In is_string_safe");

    logger.debug("resource=" + resource);

    boolean safe =
        (resource.length() - (resource.replaceAll("[^a-zA-Z0-9-_./@]+", "")).length()) == 0;

    logger.debug("Original resource name =" + resource);
    logger.debug("Replaced resource name =" + resource.replaceAll("[^a-zA-Z0-9-_./@]+", ""));
    return safe;
  }

  public void ok(HttpServerResponse resp) {
    if (!resp.closed()) {
      resp.setStatusCode(OK).end();
    }
  }

  public void accepted(HttpServerResponse resp) {
    if (!resp.closed()) {
      resp.setStatusCode(ACCEPTED).end();
    }
  }

  private void apiFailure(RoutingContext context, Throwable t) {
    logger.debug("In apifailure");
    logger.debug("Message=" + t.getMessage());
    if (t instanceof BadRequestThrowable) {
      context.response().setStatusCode(BAD_REQUEST).end(t.getMessage());
    } else if (t instanceof UnauthorisedThrowable) {
      context.response().setStatusCode(FORBIDDEN).end(t.getMessage());
    } else if (t instanceof ConflictThrowable) {
      context.response().setStatusCode(CONFLICT).end(t.getMessage());
    } else if (t instanceof InternalErrorThrowable) {
      context.response().setStatusCode(INTERNAL_SERVER_ERROR).end(t.getMessage());
    } else {
      context.fail(t);
    }
  }
}