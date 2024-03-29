package vermillion.http;

import io.reactivex.*;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.MultiMap;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.FileUpload;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.serviceproxy.ServiceException;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.quartz.*;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import vermillion.broker.reactivex.BrokerService;
import vermillion.database.Queries;
import vermillion.database.reactivex.DbService;
import vermillion.schedulers.JobScheduler;
import vermillion.schedulers.JobSchedulerListener;
import vermillion.schedulers.ProviderScheduler;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.FileNotFoundThrowable;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

public class HttpServerVerticle extends AbstractVerticle {

    public final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    // HTTP Codes
    public final int OK = 200;
    public final int CREATED = 201;
    public final int ACCEPTED = 202;
    public final int BAD_REQUEST = 400;
    public final int FORBIDDEN = 403;

    public final int CONFLICT = 409;
    public final int NOT_FOUND = 404;

    public final int INTERNAL_SERVER_ERROR = 500;

    // Auth server constants
    public final String AUTH_SERVER = System.getenv("AUTH_SERVER");
    public final String INTROSPECT_ENDPOINT = "/auth/v1/token/introspect";
    public final String WEBROOT = "webroot/";
    public final String PROVIDER_PATH = "/api-server/webroot/provider/";
    public final String AUTH_TLS_CERT_PATH = System.getenv("AUTH_TLS_CERT_PATH");
    public final String AUTH_TLS_CERT_PASSWORD = System.getenv("AUTH_TLS_CERT_PASSWORD");
    public final String WRITE_SCOPE = "write";
    public final String READ_SCOPE = "read";

    // Redis constants
    public final String REDIS_HOST = System.getenv("REDIS_HOSTNAME");
    /* Default port of redis. Port specified in the config file will
    	 not affect the default port to which redis is going to bind to
    */
    public final String REDIS_PORT = "6379";
    public final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");

    // There are 16 DBs available. Using 1 as the default database number
    public final String DB_NUMBER = "1";
    public final String CONNECTION_STR = "redis://:" + REDIS_PASSWORD + "@" + REDIS_HOST + "/" + DB_NUMBER;

    // public final String CONNECTION_STR =
    //				"redis://:" + REDIS_PASSWORD + "@" + REDIS_HOST + ":" + REDIS_PORT + "/" + DB_NUMBER;
    public final int MAX_POOL_SIZE = 10;
    public final int MAX_WAITING_HANDLERS = 32;

    // Certificate constants
    public final String SSL_CERT_NAME = System.getenv("SSL_CERT_NAME");
    public final String SSL_CERT_PASSWORD = System.getenv("SSL_CERT_PASSWORD");

    // HTTPS port
    public final int HTTPS_PORT = 443;
    // RabbitMQ default exchange to publish to
    public final String RABBITMQ_PUBLISH_EXCHANGE = "EXCHANGE";
    public String CONSUMER_PATH = "/consumer/";
    // Service Proxies
    public DbService dbService;
    public BrokerService brokerService;
    public RedisOptions options;
    public Map<String, Boolean> tokenExpiredDetails;

    @Override
    public void start(Promise<Void> startPromise) {
        logger.debug("In start");
        logger.debug("auth server=" + AUTH_SERVER);
        logger.debug("Redis constr=" + CONNECTION_STR);

        dbService = vermillion.database.DbService.createProxy(vertx.getDelegate(), "db.queue");

        tokenExpiredDetails = new HashMap<>();

        brokerService = vermillion.broker.BrokerService.createProxy(vertx.getDelegate(), "broker.queue");


        Router router = Router.router(vertx);
        final Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");

        router.route().handler(CorsHandler.create("(https://).*.")
                .allowedHeaders(allowedHeaders));
        router.route().handler(BodyHandler.create().setHandleFileUploads(true));

        // Serve API docs at /
        router.route("/").handler(StaticHandler.create("webroot"));

        router.get("/latest").handler(this::latest);
        router.post("/search").handler(this::search);

        // The path described by the regex is /consumer/<auth_server>/<token>/*
        router.routeWithRegex("\\/consumer\\/" + AUTH_SERVER + "\\/[0-9a-f]+\\/?.*")
                .handler(routingContext -> getStaticHandler(routingContext));


        // The path described by the regex is /provider/public/<domain>/<sha1>/<rs_name>/*
        router.routeWithRegex("\\/provider\\/public\\/?.*")
                .handler(StaticHandler.create()
                        .setAllowRootFileSystemAccess(false)
                        .setDirectoryListing(true));

        router.get("/download").handler(this::download);
        router.get("/downloadByQuery").handler(this::downloadByQuery);
        router.post("/providerByQuery").handler(this::providerByQuery);
        router.post("/publish").handler(this::publish);

        router.post("/search/scroll").handler(this::scrolledSearch);

        options = new RedisOptions()
                .setConnectionString(CONNECTION_STR)
                .setMaxPoolSize(MAX_POOL_SIZE)
                .setMaxWaitingHandlers(MAX_WAITING_HANDLERS);

        vertx.createHttpServer(new HttpServerOptions()
                        .setSsl(true)
                        .setCompressionSupported(true)
                        .setKeyStoreOptions(
                                new JksOptions().setPath(SSL_CERT_NAME).setPassword(SSL_CERT_PASSWORD)))
                .requestHandler(router)
                .rxListen(HTTPS_PORT)
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

    private void getStaticHandler(RoutingContext routingContext) {

        logger.debug("In existing consumer API");
        String origin = routingContext.get("origin");
        logger.debug("Origin:" + origin);
        boolean isOriginValid = "download".equalsIgnoreCase(origin);
        logger.debug("Is origin valid:" + isOriginValid);
        StaticHandler staticHandler;
        if(isOriginValid) {
            staticHandler = StaticHandler.create()
                    .setAllowRootFileSystemAccess(false)
                    .setDirectoryListing(true);
            staticHandler.handle(routingContext);
            return;
        }
        HttpServerRequest request = routingContext.request();
        String userAgent = request.getHeader("User-Agent");
        logger.debug("User agent is:" + userAgent);

        String token = getFinalTokenFromNormalisedPath(routingContext);
        logger.debug("Final token:" + token);

        Single<Integer> tokenExpiry = isTokenExpired(token);

        Path finalConsumerResourcePath = Paths.get(WEBROOT + "consumer/" + token);
        File fileToBeDeleted = new File(String.valueOf(finalConsumerResourcePath));
        logger.debug("File directory to be deleted:" + fileToBeDeleted);
        boolean doesFileExist = Files.exists(fileToBeDeleted.toPath());
        logger.debug("Does file exists:" + doesFileExist);

        String FINAL_CONSUMER_PATH = "/consumer/" + token ;
        tokenExpiry.subscribe(result -> {
            logger.debug("Value of token:" + result);
            if (result != null && result > 0 && doesFileExist) {
                boolean isFileSymLinkDeleted = deleteDirectory(fileToBeDeleted);
                logger.debug("Is file directory deleted:" + isFileSymLinkDeleted);
                apiFailure(routingContext, new UnauthorisedThrowable("The access token is expired. So, please obtain a new access token"));
                return;
            }
            if (!doesFileExist && result != null && result > 0) {
                logger.info("The file requested is deleted as token is expired");
                apiFailure(routingContext, new UnauthorisedThrowable("The access token is expired. So, please obtain a new access token"));
                return;
            }
            logger.debug("Consumer path :" + FINAL_CONSUMER_PATH);
            StaticHandler staticHandlerForConsumer = StaticHandler.create()
                    .setAllowRootFileSystemAccess(false)
                    .setDirectoryListing(true);
            staticHandlerForConsumer.handle(routingContext);

        }, t -> apiFailure(routingContext, t));
    }

    private String getFinalTokenFromNormalisedPath(RoutingContext routingContext) {
        String path = routingContext.normalisedPath();
        logger.debug("Normalized path:" + path);
        Map<Integer, String> paramsMap = new HashMap<>();
        String[] pathParams = path.split("/");
        int counter = 1;
        for (String param: pathParams) {
            paramsMap.put(counter++, param);
        }
        String authServer = paramsMap.get(3);
        String tokenHash = paramsMap.get(4);
        String token = authServer + "/" + tokenHash;
        logger.debug("Final paramsMap: " + paramsMap);
        return token;
    }

    boolean deleteDirectory(File directoryToBeDeleted )  {
        File[] filesInTheDirectory = directoryToBeDeleted.listFiles();
        if (filesInTheDirectory != null) {
            for (File file : filesInTheDirectory) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private Single<Integer> isTokenExpired(String token) {
        logger.debug("In token expiry method " + token);
        Single<String> tokenDetailsFromRedis =  getValue(token);

        return tokenDetailsFromRedis.flatMapCompletable(
                tokenDetailsFromCache -> "absent".equalsIgnoreCase(tokenDetailsFromCache) ? introspect(token, null) : Completable.complete())
                .andThen(Single.defer(() -> getValue(token)))
                .flatMapMaybe(result -> "absent".equalsIgnoreCase(result)
                        ? Maybe.empty()
                        : Maybe.just(new JsonObject(result).getString("expiry")))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMap(tokenExpiry -> {          // handle the empty scenario from above flatMapMaybe
                    if(tokenExpiry.isPresent()) {
                        logger.debug("tokenExpiry:" + tokenExpiry.get());
                        String currentDate = Clock.systemUTC().instant().toString();
                        if (currentDate.compareTo(tokenExpiry.get()) > 0) {
                            tokenExpiredDetails.put("tokenExpiredDetails", true);
                        } else {
                            tokenExpiredDetails.put("tokenExpiredDetails", false);
                        }
                        logger.debug("tokenExpiredDetails: " + tokenExpiredDetails.toString());
                        return Single.just(currentDate.compareTo(tokenExpiry.get()));
                    }
                   return Single.error(new UnauthorisedThrowable("The access token details are not present in cache"));
                });
    }

    // TODO: Check why using a custom conf file fails here
    public Single<RedisAPI> getRedisClient() {
        logger.debug("In get redis client");
        logger.debug("options=" + options.toJson().encodePrettily());
        return Redis.createClient(vertx, options).rxConnect().map(RedisAPI::api);
    }

    public Single<String> getValue(String key) {

        logger.debug("In getValue");

        // TODO: Initiate introspect here instead of calling the API twice
        return getRedisClient()
                .flatMapMaybe(redisAPI -> {
                    logger.debug("Got redis client");
                    return redisAPI.rxGet(key);
                })
                .map(value -> {
                    logger.debug("Value=" + value.toString());
                    return Optional.of(value);
                })
                .toSingle(Optional.empty())
                .map(value -> value.isPresent() ? value.get().toString() : "absent");
    }

    public Completable setValue(String key, String value) {

        logger.debug("In set value");
        ArrayList<String> list = new ArrayList<>();

        list.add(key);
        list.add(value);

        return getRedisClient().flatMapCompletable(redisAPI -> Completable.fromMaybe(redisAPI.rxSet(list)));
    }

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
            apiFailure(context, new BadRequestThrowable("No resource ID found in request"));
            return;
        }

        if (!isValidResourceID(resourceID)) {
            logger.debug("Resource is is malformed");
            apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
            return;
        }

        if (!resourceID.endsWith(".public") && token == null) {
            logger.debug("Id is secure but no access token provided");
            apiFailure(context, new BadRequestThrowable("No access token found in request"));
            return;
        }

        if (!resourceID.endsWith(".public") && token != null && !isValidToken(token)) {
            logger.debug("Access token is malformed");
            apiFailure(context, new BadRequestThrowable("Malformed access token"));
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
            checkAuthorisation(token, READ_SCOPE, requestedIDs, null)
                    .andThen(Single.defer(() -> dbService.rxSecureSearch(constructedQuery, token, false, null)))
                    .subscribe(
                            result -> response.putHeader("content-type", "application/json")
                                    .end(result.encode()),
                            t -> apiFailure(context, t));
        }
    }

    // TODO: Should the scroll API need special permissions?
    // After all, it puts quite a bit of load on the server for large responses
    public void scrolledSearch(RoutingContext context) {
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        JsonObject requestBody;
        int scrollValue;

        String token = request.getParam("token");

        if (token != null && !isValidToken(token)) {
            apiFailure(context, new UnauthorisedThrowable("Invalid Token"));
            return;
        }

        try {
            requestBody = context.getBodyAsJson();

            if (requestBody == null) {
                apiFailure(context, new BadRequestThrowable("Body is empty"));
                return;
            }

        } catch (Exception e) {
            apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
            return;
        }

        logger.debug("Body=" + requestBody.encode());

        if (!requestBody.containsKey("scroll_id")) {
            apiFailure(context, new BadRequestThrowable("Obtain a scroll_id from the search API first"));
            return;
        }

        if (!requestBody.containsKey("scroll_duration")) {
            apiFailure(context, new BadRequestThrowable("Scroll duration not specified"));
            return;
        }

        Set<String> permittedFieldSet = new HashSet<>();
        permittedFieldSet.add("scroll_id");
        permittedFieldSet.add("scroll_duration");

        if (!permittedFieldSet.containsAll(requestBody.fieldNames())) {
            apiFailure(context, new BadRequestThrowable("Body contains unnecessary fields"));
            return;
        }

        Object scrollIdObj = requestBody.getValue("scroll_id");

        if (!(scrollIdObj instanceof String)) {
            apiFailure(context, new BadRequestThrowable("Scroll ID is not valid"));
            return;
        }

        Object scrollDurationObj = requestBody.getValue("scroll_duration");

        if (!(scrollDurationObj instanceof String)) {
            apiFailure(context, new BadRequestThrowable("Scroll Duration is not valid"));
            return;
        }

        String scrollId = requestBody.getString("scroll_id");

        String scrollDuration = requestBody.getString("scroll_duration");

        if ("".equals(scrollId)) {
            apiFailure(context, new BadRequestThrowable("Scroll Id is empty"));
            return;
        }

        //        if(!isValidScrollID(scrollId)){
        //            apiFailure(context, new BadRequestThrowable("Invalid Scroll Id"));
        //            return;
        //        }

        if ("".equals(scrollDuration)) {
            apiFailure(context, new BadRequestThrowable("Scroll Duration is empty"));
            return;
        }

        String scrollUnit = scrollDuration.substring(scrollDuration.length() - 1);
        String scrollValueStr = scrollDuration.substring(0, scrollDuration.length() - 1);

        try {
            scrollValue = NumberUtils.createInteger(scrollValueStr);
        } catch (NumberFormatException numberFormatException) {
            apiFailure(context, new BadRequestThrowable("Scroll value is not a valid integer"));
            return;
        }

        if (scrollValue <= 0) {
            apiFailure(context, new BadRequestThrowable("Scroll value cannot be less than or equal to zero"));
            return;
        }

        if ((scrollUnit.equalsIgnoreCase("h") && scrollValue != 1)
                || (scrollUnit.equals("m") && scrollValue > 60)
                || (scrollUnit.equalsIgnoreCase("s") && scrollValue > 3600)) {
            apiFailure(
                    context,
                    new BadRequestThrowable(
                            "Scroll value is too large. Max scroll duration cannot be more than 1 hour"));
            return;
        } else if (!scrollUnit.equalsIgnoreCase("h") && !scrollUnit.equals("m") && !scrollUnit.equalsIgnoreCase("s")) {
            apiFailure(context, new BadRequestThrowable("Scroll unit is invalid"));
            return;
        }

        if (token != null) {
            checkAuthorisation(token, "read")
                    .flatMap(
                            authorisedIDs -> dbService.rxScrolledSearch(scrollId, scrollDuration, token, authorisedIDs))
                    .subscribe(
                            result -> response.putHeader("content-type", "application/json")
                                    .end(result.encode()),
                            t -> apiFailure(context, t));
        } else {
            dbService
                    .rxScrolledSearch(scrollId, scrollDuration, null, null)
                    .subscribe(
                            result -> response.putHeader("content-type", "application/json")
                                    .end(result.encode()),
                            t -> apiFailure(context, t));
        }
    }

    public void search(RoutingContext context) {
        // TODO: Convert all types of responses to JSON

        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();
        JsonObject requestBody;

        // Init a variable to check if scrolling has been requested
        boolean scroll = false;

        // Read the token if present
        String token = request.getParam("token");

        if (token != null && !isValidToken(token)) {
            apiFailure(context, new UnauthorisedThrowable("Invalid Token"));
            return;
        }

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

            if (requestBody == null) {
                apiFailure(context, new BadRequestThrowable("Body is empty"));
                return;
            }
        } catch (Exception e) {
            apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
            return;
        }

        logger.debug("Body=" + requestBody.encode());

        if (!requestBody.containsKey("id")) {
            apiFailure(context, new BadRequestThrowable("No id found in body"));
            return;
        }

        if (!requestBody.containsKey("geo_distance")
                && !requestBody.containsKey("time")
                && !requestBody.containsKey("attribute")) {
            apiFailure(context, new BadRequestThrowable("Invalid request"));
            return;
        }

        Set<String> permittedFieldSet = new HashSet<>();
        permittedFieldSet.add("id");
        permittedFieldSet.add("geo_distance");
        permittedFieldSet.add("time");
        permittedFieldSet.add("attribute");
        permittedFieldSet.add("size");
        permittedFieldSet.add("scroll_duration");

        if (!permittedFieldSet.containsAll(requestBody.fieldNames())) {
            apiFailure(context, new BadRequestThrowable("Body contains unnecessary fields"));
            return;
        }

        Object resourceIdObj = requestBody.getValue("id");

        if (!(resourceIdObj instanceof String) && !(resourceIdObj instanceof JsonArray)) {
            apiFailure(context, new BadRequestThrowable("Resource id is not valid"));
            return;
        }
        if (resourceIdObj instanceof JsonArray) {
            // Resource ID is an array of strings
            resourceIDArray = requestBody.getJsonArray("id");
            for (Object o : resourceIDArray) {
                if (!(o instanceof String)) {
                    apiFailure(context, new BadRequestThrowable("Resource ID list should be a list of strings"));
                    return;
                }
                if ("".equalsIgnoreCase(o.toString())) {
                    apiFailure(context, new BadRequestThrowable("Resource ID is empty"));
                    return;
                }
                if (!isValidResourceID(o.toString())) {
                    apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
                    return;
                }
                if (!(((String) o).endsWith(".public")) && token == null) {
                    apiFailure(context, new BadRequestThrowable("No token found in request"));
                    return;
                }
            }
            termsQuery.getJsonObject("terms").put("id.keyword", resourceIDArray);
            filterQuery.add(termsQuery);
        } else {
            // Standalone resource ID
            resourceIDstr = requestBody.getString("id");
            if ("".equalsIgnoreCase(resourceIDstr)) {
                apiFailure(context, new BadRequestThrowable("Resource ID is empty"));
                return;
            }
            if (!isValidResourceID(resourceIDstr)) {
                apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
                return;
            }
            if (!resourceIDstr.endsWith(".public") && token == null) {
                apiFailure(context, new BadRequestThrowable("No token found in request"));
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
                apiFailure(context, new BadRequestThrowable("Response size should be an integer"));
                return;
            }

            try {
                size = NumberUtils.createInteger(sizeObj.toString());
            } catch (NumberFormatException numberFormatException) {
                apiFailure(context, new BadRequestThrowable("Response size is not a valid integer"));
                return;
            }

            if (size <= 0 || size > 10000) {
                apiFailure(context, new BadRequestThrowable("Response size must be between 1-10000"));
                return;
            }
        }

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
                        context, new BadRequestThrowable("Geo distance does not contain coordinates and/or distance"));
                return;
            }

            Object distanceObj = geoDistance.getValue("distance");

            if (!(distanceObj instanceof String)) {
                apiFailure(context, new BadRequestThrowable("Distance is not a string"));
                return;
            }
            String distance = geoDistance.getString("distance");

            if (!distance.endsWith("m")) {
                apiFailure(
                        context,
                        new BadRequestThrowable(
                                "Only metres are supported. Use the raw query interface for other units"));
                return;
            }

            String distanceQuantity = distance.substring(0, distance.length() - 1);

            logger.debug("Is a valid number ?" + NumberUtils.isCreatable(distanceQuantity));

            // If the number preceding m, km, cm etc is a valid number
            int geoDistanceQuantity;
            try {
                geoDistanceQuantity = Integer.parseInt(distanceQuantity);
                if (geoDistanceQuantity < 1) {
                    apiFailure(context, new BadRequestThrowable("Distance less than 1m"));
                    return;
                }
            } catch (NumberFormatException ex) {
                apiFailure(context, new BadRequestThrowable("Distance is not valid."));
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

            if ((coordinates.getValue(0) instanceof String) || (coordinates.getValue(1) instanceof String)) {
                apiFailure(context, new BadRequestThrowable("Coordinates are not valid numbers"));
                return;
            }

            logger.debug("Coordinates lat check = "
                    + NumberUtils.isCreatable(coordinates.getValue(0).toString()));
            logger.debug("Coordinates lon check = "
                    + NumberUtils.isCreatable(coordinates.getValue(1).toString()));

            if (!NumberUtils.isCreatable(coordinates.getValue(0).toString())
                    || !NumberUtils.isCreatable(coordinates.getValue(1).toString())) {
                apiFailure(context, new BadRequestThrowable("Coordinates should be valid numbers"));
                return;
            }

            double lat = coordinates.getDouble(0);
            double lon = coordinates.getDouble(1);

            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                apiFailure(context, new BadRequestThrowable("Invalid coordinates"));
                return;
            }

            geoQuery.getJsonObject("geo_distance").put("distance", distance).put("coordinates", coordinates);

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

            if (!(startObj instanceof String) || !(endObj instanceof String)) {
                apiFailure(context, new BadRequestThrowable("Start and end objects are not strings"));
                return;
            }

            String start = time.getString("start");
            String end = time.getString("end");
            //            Locale locale = new Locale("English", "IN");
            //
            //            if (!GenericValidator.isDate(start, locale) || !GenericValidator.isDate(end, locale)) {
            //                apiFailure(context, new BadRequestThrowable("Start and/or end strings are not valid
            // dates"));
            //                return;
            //            }
            //
            // /(\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d\.\d+([+-][0-2]\d:[0-5]\d|Z))|(\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d([+-][0-2]\d:[0-5]\d|Z))|(\d{4}-[01]\d-[0-3]\dT[0-2]\d:[0-5]\d([+-][0-2]\d:[0-5]\d|Z))/

            DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser();

            DateTime startDate;
            DateTime endDate;
            try {
                startDate = fmt.parseDateTime(start);
                endDate = fmt.parseDateTime(end);
                if (endDate.getMillis() - startDate.getMillis() < 0) {
                    apiFailure(context, new BadRequestThrowable("End date is smaller than start date"));
                    return;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                apiFailure(context, new BadRequestThrowable("Start and/or end strings are not valid dates"));
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

            String attributeName = attribute.getString("term").trim();

            if ("".equals(attributeName)) {
                apiFailure(context, new BadRequestThrowable("Term parameter is empty"));
                return;
            }

            if (!(attribute.containsKey("min") && attribute.containsKey("max"))
                    == !(attribute.containsKey("term") && attribute.containsKey("value"))) {

                apiFailure(context, new BadRequestThrowable("Invalid attribute query"));
                return;
            }

            // TODO: Add a case where only min or max are provided. Not both
            // Case 1: When the attribute query is a number
            if (attribute.containsKey("min") && attribute.containsKey("max")) {

                Object minObj = attribute.getValue("min");
                Object maxObj = attribute.getValue("max");

                if (minObj instanceof String || maxObj instanceof String) {
                    apiFailure(context, new BadRequestThrowable("Min and max values should be numbers"));
                    return;
                }

                if (!NumberUtils.isCreatable(minObj.toString()) || !NumberUtils.isCreatable(maxObj.toString())) {
                    apiFailure(context, new BadRequestThrowable("Min and max values are not valid numbers"));
                    return;
                }

                Double min = attribute.getDouble("min");
                Double max = attribute.getDouble("max");

                if (min > max) {
                    apiFailure(context, new BadRequestThrowable("Min value is greater than max"));
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
                    apiFailure(context, new BadRequestThrowable("Value is not a valid string"));
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
                apiFailure(context, new BadRequestThrowable("Scroll parameter must be a string"));
                return;
            }

            scrollStr = scrollObj.toString();
            if ("".equals(scrollStr)) {
                apiFailure(context, new BadRequestThrowable("Scroll parameter is empty"));
                return;
            }
            // If the value is 10m, separate out '10' and 'm'
            scrollUnit = scrollStr.substring(scrollStr.length() - 1);
            scrollValueStr = scrollStr.substring(0, scrollStr.length() - 1);

            try {
                scrollValue = NumberUtils.createInteger(scrollValueStr);
            } catch (NumberFormatException numberFormatException) {
                apiFailure(context, new BadRequestThrowable("Scroll value is not a valid integer"));
                return;
            }

            if (scrollValue <= 0) {
                apiFailure(context, new BadRequestThrowable("Scroll value cannot be less than or equal to zero"));
                return;
            }

            if ((scrollUnit.equalsIgnoreCase("h") && scrollValue != 1)
                    || (scrollUnit.equals("m") && scrollValue > 60)
                    || (scrollUnit.equalsIgnoreCase("s") && scrollValue > 3600)) {
                apiFailure(
                        context,
                        new BadRequestThrowable(
                                "Scroll value is too large. Max scroll duration cannot be more than 1 hour"));
                return;
            } else if (!scrollUnit.equalsIgnoreCase("h")
                    && !scrollUnit.equals("m")
                    && !scrollUnit.equalsIgnoreCase("s")) {
                apiFailure(context, new BadRequestThrowable("Scroll unit is invalid"));
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
                                t -> apiFailure(context, t));
            } else {
                dbService
                        .rxSearch(baseQuery, false, null)
                        .subscribe(
                                result -> response.putHeader("content-type", "application/json")
                                        .end(result.encode()),
                                t -> apiFailure(context, t));
            }
        } else {
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
                checkAuthorisation(token, READ_SCOPE, requestedIDs, null)
                        .andThen(Single.defer(() -> dbService.rxSecureSearch(baseQuery, token, true, finalScrollStr)))
                        .subscribe(
                                result -> response.putHeader("content-type", "application/json")
                                        .end(result.encode()),
                                t -> apiFailure(context, t));
            } else {
                checkAuthorisation(token, READ_SCOPE, requestedIDs, null)
                        .andThen(Single.defer(() -> dbService.rxSecureSearch(baseQuery, token, false, null)))
                        .subscribe(
                                result -> response.putHeader("content-type", "application/json")
                                        .end(result.encode()),
                                t -> apiFailure(context, t));
            }
        }
    }

    // TODO: If Id is provided, reroute to the specific file
    public void download(RoutingContext context) {

        HttpServerRequest request = context.request();
        context.put("origin", "download");

        // If token is valid for resources apart from secure 'files' then specify list of ids in the
        // request
        String token = request.getParam("token");
        String idParam = request.getParam("id");

        logger.debug("token=" + token);

        if (token == null) {
            apiFailure(context, new BadRequestThrowable("No access token found in request"));
            return;
        }

        if (!isValidToken(token)) {
            apiFailure(context, new UnauthorisedThrowable("Malformed access token"));
            return;
        }

        JsonArray requestedIds = new JsonArray();
        String basePath = PROVIDER_PATH + "secure/";

        if (idParam != null) {
            // TODO: Handle possible issues here
            Arrays.asList(idParam.split(",")).forEach(requestedIds::add);
        }

        for (int i = 0; i < requestedIds.size(); i++) {

            String resourceIDStr = requestedIds.getString(i);

            if (!isValidResourceID(resourceIDStr)) {
                apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
                return;
            }

            if (resourceIDStr.endsWith(".public")) {
                apiFailure(
                        context,
                        new BadRequestThrowable(
                                "This API is for secure resources only. Use /provider/public endpoint to explore public data"));
                return;
            }
        }

        logger.debug("Requested IDs Json =" + requestedIds.encode());

        CONSUMER_PATH = "/consumer/" + token + "/";
        isTokenExpired(token).subscribe();
        // TODO: Avoid duplication here
        if (idParam == null) {
            checkAuthorisation(token, READ_SCOPE)
                    // TODO: Rxify this further
                    .flatMapCompletable(authorisedIds -> {
                        logger.debug("Authorised IDs = " + authorisedIds.encode());

                        for (int i = 0; i < authorisedIds.size(); i++) {
                            logger.debug("File=" + basePath + authorisedIds.getString(i));

                            if (Files.notExists(Paths.get(basePath + authorisedIds.getString(i)))) {
                                return Completable.error(
                                        new UnauthorisedThrowable("Requested resource ID(s) is not present"));
                            }
                        }
                        Path consumerResourcePath = null;
                        for (int i = 0; i < authorisedIds.size(); i++) {

                            String resourceId = authorisedIds.getString(i);
                            // Get the actual file name on disk

                            String consumerResourceDir = WEBROOT + "consumer/" + token + "/" + resourceId;

                            // Create consumer directory path if it does not exist
                            new File(consumerResourceDir).mkdirs();
                            logger.debug("Created consumer subdirectory");

                            consumerResourcePath = Paths.get(WEBROOT + "consumer/" + token + "/" + resourceId);
                            Path providerResourcePath = Paths.get(basePath + resourceId);

                            // TODO: This could take a very long time for multiple large files
                            try {
                                Files.createSymbolicLink(consumerResourcePath, providerResourcePath);
                            } catch (FileAlreadyExistsException ignored) {

                            } catch (Exception e) {
                                return Completable.error(new InternalErrorThrowable("Could not create symlinks"));
                            }
                            logger.debug("tokenExpiredDetails : " + tokenExpiredDetails.toString());
                            if (tokenExpiredDetails.get("tokenExpiredDetails")) {
                                Files.deleteIfExists(consumerResourcePath);
                            }
                        }

                        if(tokenExpiredDetails.get("tokenExpiredDetails")) {
                            return Completable.error(new UnauthorisedThrowable("Unauthorised due to token expiry"));
                        }

                        // Appending Consumer Path
                        if (!authorisedIds.isEmpty()) {

                            // If only one id is there return the file.
                            if (authorisedIds.size() == 1) {
                                CONSUMER_PATH += authorisedIds.getString(0) + "/";
                            } else {

                                // Getting common directory for all ids
                                String authorisedIdPrefix = commonPrefix(authorisedIds);
                                int lastDirIndex = authorisedIdPrefix.lastIndexOf('/');
                                if (lastDirIndex != -1) {
                                    CONSUMER_PATH += authorisedIdPrefix.substring(0, lastDirIndex + 1);
                                }
                            }
                        }

                        return Completable.complete();
                    })
                    .subscribe(() -> context.reroute(CONSUMER_PATH), t -> apiFailure(context, t));
        } else {
            checkAuthorisation(token, READ_SCOPE, requestedIds, null)
                    .andThen(Completable.fromCallable(() -> {
                        logger.debug("Requested IDs = " + requestedIds.encode());
                        for (int i = 0; i < requestedIds.size(); i++) {
                            logger.debug("File=" + basePath + requestedIds.getString(i));
                            if (Files.notExists(Paths.get(basePath + requestedIds.getString(i)))) {
                                return Completable.error(
                                        new UnauthorisedThrowable("Requested resource ID(s) is not present"));
                            }
                        }
                        Path consumerResourcePath = null;

                        for (int i = 0; i < requestedIds.size(); i++) {
                            String resourceId = requestedIds.getString(i);
                            String consumerResourceDir = WEBROOT + "consumer/" + token + "/"
                                    + resourceId.substring(0, resourceId.lastIndexOf('/'));

                            // Create consumer directory path if it does not exist
                            new File(consumerResourceDir).mkdirs();
                            logger.debug("Insubdirectory");

                            consumerResourcePath = Paths.get(WEBROOT + "consumer/" + token + "/" + resourceId);
                            Path providerResourcePath = Paths.get(basePath + resourceId);

                            // TODO: This could take a very long time for multiple large files
                            try {
                                Files.createSymbolicLink(consumerResourcePath, providerResourcePath);
                            } catch (FileAlreadyExistsException ignored) {

                            } catch (Exception e) {
                                return Completable.error(new InternalErrorThrowable("Could not create symlinks"));
                            }
                        }

                        // Appending Consumer Path
                        if (!requestedIds.isEmpty()) {
                            if (requestedIds.size() == 1) {
                                CONSUMER_PATH += requestedIds.getString(0) + "/";
                            } else {
                                String requestedIdPrefix = commonPrefix(requestedIds);
                                int lastDirIndex = requestedIdPrefix.lastIndexOf('/');
                                if (lastDirIndex != -1) {
                                    CONSUMER_PATH += requestedIdPrefix.substring(0, lastDirIndex + 1);
                                }
                            }
                        }

                        return Completable.complete();
                    }))
                    .andThen(Completable.defer(()-> {
                        logger.debug("tokenExpiredDetails : " + tokenExpiredDetails.toString());
                        for (int i = 0; i < requestedIds.size(); i++) {
                            String resourceId = requestedIds.getString(i);
                            if (tokenExpiredDetails.get("tokenExpiredDetails")) {
                                Path consumerResourcePath = Paths.get(WEBROOT + "consumer/" + token + "/" + resourceId);
                                Files.deleteIfExists(consumerResourcePath);
                            }
                        }
                        if (tokenExpiredDetails.get("tokenExpiredDetails")) {
                            return Completable.error(new UnauthorisedThrowable("Unauthorised due to token expiry"));
                        }
                        return Completable.complete();
                    }))
                    .subscribe(() -> context.reroute(CONSUMER_PATH), t -> apiFailure(context, t));
        }
    }

    // Publish API for timeseries data as well as static files
    public void publish(RoutingContext context) {

        logger.debug("In publish API");
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        FileUpload file = null;
        FileUpload metadata = null;
        JsonObject metaJson = null;

        String fileName = null;
        String resourceId;
        String token;
        JsonObject requestBody = null;

        HashMap<String, FileUpload> fileUploads = new HashMap<>();
        List<FileUpload> fileUploadList = new ArrayList<>();

        logger.debug("File uploads = " + context.fileUploads().size());
        logger.debug("Is empty = " + context.fileUploads().isEmpty());
        if (!context.fileUploads().isEmpty()) {
            context.fileUploads().forEach(f -> {
                fileUploads.put(f.name(), f);
                fileUploadList.add(f);
                logger.debug(f.name() + " = " + fileUploads.get(f.name()).uploadedFileName());
            });

        }

        // TODO: Check for invalid IDs
        resourceId = request.getParam("id");
        token = request.getParam("token");
        if (resourceId == null) {
            apiFailure(context, new BadRequestThrowable("No resource ID found in request"));
            deleteUploads(fileUploadList);
            return;
        }
        if (token == null) {
            apiFailure(context, new BadRequestThrowable("No access token found in request"));
            deleteUploads(fileUploadList);
            return;
        }

        if (!isValidToken(token) || !isValidResourceID(resourceId)) {
            apiFailure(context, new UnauthorisedThrowable("Malformed resource ID or token"));
            deleteUploads(fileUploadList);
            return;
        }

        String[] splitId = resourceId.split("/");

        // TODO: Need to define the types of IDs supported

        // Rationale: resource ids are structured as domain/sha1/rs.com/category/id
        // Since pre-checks have been done, it is safe to get splitId[3]
        String category = splitId[3];

        JsonArray requestedIdList = new JsonArray().add(resourceId);

        if (context.fileUploads().size() > 0) {

            if (context.fileUploads().size() > 2 || !fileUploads.containsKey("file")) {
                apiFailure(context, new BadRequestThrowable("Too many files and/or missing 'file' parameter"));
                // Delete uploaded files if inputs are not as required
                deleteUploads(fileUploadList);
                return;
            } else {
                file = fileUploads.get("file");

                if (fileUploads.containsKey("metadata")) {
                    metadata = fileUploads.get("metadata");

                    // TODO: Rxify this
                    // TODO: File size could crash server. Need to handle this
                    Buffer metaBuffer = vertx.fileSystem().readFileBlocking(metadata.uploadedFileName());

                    try {
                        metaJson = metaBuffer.toJsonObject();
                    } catch (Exception e) {
                        apiFailure(context, new BadRequestThrowable("Metadata is not a valid JSON"));
                        deleteUploads(fileUploadList);
                        return;
                    }
                    logger.debug("Metadata = " + metaJson.encode());
                } else {
                    // Delete all other files except 'file' and 'metadata'
                    fileUploads.forEach((k, v) -> {
                        if (!"file".equalsIgnoreCase(k)) {
                            try {
                                Files.deleteIfExists(Paths.get(v.uploadedFileName()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        }

        if (file != null) {

            fileName = file.uploadedFileName();

            String finalFileName = fileName;

            // If ID = domain/sha/rs.com/category/id, then create dir structure only until category
            // if it does not already exist
            String accessFolder = PROVIDER_PATH + (resourceId.endsWith(".public") ? "public/" : "secure/");

            String providerDirStructure = accessFolder + resourceId;
            logger.debug("Provider dir structure=" + providerDirStructure);

            String providerFilePath = accessFolder + resourceId + "/" + file.fileName();
            logger.debug("Provider file path=" + providerFilePath);

            logger.debug("Source=" + finalFileName);
            logger.debug("Destination=" + providerFilePath);

            String fileLink = null;

            if (resourceId.endsWith(".public")) {
                fileLink = providerFilePath;
            } else {
                fileLink = "/download";
            }

            JsonObject dbEntryJson = new JsonObject()
                    .put("data", new JsonObject().put("link", fileLink).put("filename", file.fileName()))
                    .put("timestamp", Clock.systemUTC().instant().toString())
                    .put("id", resourceId)
                    .put("category", category);

            if (metaJson != null) {
                logger.debug("Metadata is not null");
                // TODO: Cap size of metadata
                dbEntryJson.getJsonObject("data").put("metadata", metaJson);
                try {
                    logger.debug("Metadata path = " + metadata.uploadedFileName());
                    Files.deleteIfExists(Paths.get(metadata.uploadedFileName()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            checkAuthorisation(token, WRITE_SCOPE, requestedIdList, fileUploadList)
                    .andThen(Completable.defer(() -> {
                        new File(providerDirStructure).mkdirs();
                        try {
                            Files.move(
                                    Paths.get(finalFileName),
                                    Paths.get(providerFilePath),
                                    StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            return Completable.error(new InternalErrorThrowable("Could not move files"));
                        }
                        return Completable.complete();
                    }))
                    .andThen(brokerService.rxAdminPublish(RABBITMQ_PUBLISH_EXCHANGE, resourceId, dbEntryJson.encode()))
                    .subscribe(() -> response.setStatusCode(201).end("Ok"), t -> apiFailure(context, t));
            return;
        }
        // For timeseries data
        else {
            try {
                requestBody = context.getBodyAsJson();
            } catch (Exception e) {
                apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
                return;
            }

            if (requestBody == null) {
                apiFailure(context, new BadRequestThrowable("Body is null"));
                return;
            }

            /* Data should be of the form
            	 {"data": object, "timestamp": timestamp, "coordinates": [lon, lat],
            	 "id" (populated by publish API): resource-id,
            	 "category"(populated by publish API): category}
            */

            Set<String> permittedFieldSet = new HashSet<>();
            permittedFieldSet.add("data");
            permittedFieldSet.add("timestamp");
            permittedFieldSet.add("coordinates");

            if (!requestBody.containsKey("data")) {
                apiFailure(context, new BadRequestThrowable("No data field in body"));
                return;
            }
            if (!(requestBody.getValue("data") instanceof JsonObject)) {
                apiFailure(context, new BadRequestThrowable("Data field is not a JSON object"));
                return;
            }
            if (!permittedFieldSet.containsAll(requestBody.fieldNames())) {
                apiFailure(context, new BadRequestThrowable("Body contains unnecessary fields"));
                return;
            }

            if (!requestBody.containsKey("timestamp")) {
                requestBody.put("timestamp", Clock.systemUTC().instant().toString());
            }

            requestBody.put("id", resourceId);
            requestBody.put("category", category);
            requestBody.put("mime-type", "application/json");
        }

        JsonObject finalRequestBody = requestBody;

        // There is no need for introspect here. It will be done at the rmq auth backend level
        brokerService
                .rxPublish(token, RABBITMQ_PUBLISH_EXCHANGE, resourceId, finalRequestBody.encode())
                .subscribe(() -> response.setStatusCode(202).end(), t -> apiFailure(context, t));

        logger.debug("Filename = " + fileName);
    }

    public void providerByQuery(RoutingContext context) {

        logger.debug("In Provider By Query");
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();
        MultiMap parameters = request.params();
        String resourceId = request.getParam("id");

        if (resourceId != null && !isValidResourceID(resourceId)) {
            apiFailure(context, new UnauthorisedThrowable("Invalid Id"));
            return;
        }

        if (resourceId != null && !resourceId.endsWith(".public")) {
            apiFailure(context, new UnauthorisedThrowable("This is used only for public datasets and not secure ones"));
        }

        JsonObject requestBody;
        try {
            requestBody = context.getBodyAsJson();
            if (requestBody == null) {
                apiFailure(context, new BadRequestThrowable("Body is empty"));
                return;
            }
        } catch (Exception e) {
            apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
            return;
        }

        if (!requestBody.containsKey("email")) {
            apiFailure(context, new BadRequestThrowable("Please provide email Id"));
            return;
        }
        String email = requestBody.getString("email");
        if (!isEmailValid(email)) {
            apiFailure(context, new BadRequestThrowable("Incorrect email Id"));
            return;
        }

        logger.debug("Body=" + requestBody.encode());
        Queries query = new Queries();

        int size = 10000;
        JsonObject providerByQuery = query.getProviderByQuery();
        JsonArray jsonArray = providerByQuery.put("size", size).getJsonObject("query").getJsonObject("bool")
                .getJsonArray("filter");

        if (resourceId != null) {
            jsonArray.add(new JsonObject().put("terms",
                            new JsonObject().put("id.keyword",
                                    new JsonArray().add(resourceId))));
        }
        List<Map.Entry<String, String>> entries = parameters.entries();
        JsonObject subCategoryEntryField = new JsonObject();
        for (int i = 0; i < entries.size(); i++) {
            String key;
            String value;
            if ("category".equalsIgnoreCase(entries.get(i).getKey())) {
                key = entries.get(i).getKey();
                value = entries.get(i).getValue();
                if ( !"all".equalsIgnoreCase(value) && !"".equalsIgnoreCase(value)) {
                    jsonArray.add(new JsonObject().put("match", new JsonObject().put(key + ".keyword", value)));
                }
            } else if (!entries.get(i).getKey().equalsIgnoreCase("token")
                    && !"id".equalsIgnoreCase(entries.get(i).getKey())) {
                key = entries.get(i).getKey();
                value = entries.get(i).getValue();
                logger.debug("key: " + key);
                logger.debug("value: " + value);
                if ( !"all".equalsIgnoreCase(value) && !"".equalsIgnoreCase(value)) {
                    jsonArray.add(new JsonObject().put("match", new JsonObject().put("data.metadata." + key + ".keyword", value)));
                }
                if ("sub_category".equalsIgnoreCase(key)) {
                    subCategoryEntryField.put(key, value);
                }
            }
        }

        logger.debug("subCategoryEntryField = " + subCategoryEntryField.encodePrettily());
        logger.debug("provider by query =" + query.getProviderByQuery().encodePrettily());

        if (jsonArray.size() == 0) {
            apiFailure(context, new BadRequestThrowable("Please provide the query parameters to download the files"));
            return;
        }
        UUID uuid = UUID.randomUUID();
        List<String> listOfEligibleIds = new ArrayList<>();
        List<String> finalZipLinks = new ArrayList<>();
        logger.debug("server host:" + System.getenv("SERVER_NAME"));
        AtomicReference<List<String>> distinctIds = new AtomicReference<>();
        List<String> listOfIdsNeedToBeSentToScheduler = new ArrayList<>();
        List<File> listOfFilesNeedToBeZipped = new ArrayList<>();
//        List<File> listOfFilesNeedToBeAddedToZipFile = new ArrayList<>();
        AtomicReference<List<String>> finalListOfIdsNeedToBeSentToScheduler = new AtomicReference<>();
        AtomicReference<JsonArray> hits = new AtomicReference<>();
        AtomicBoolean didResponseEnded = new AtomicBoolean(false);
        AtomicInteger noOfScrollCalls = new AtomicInteger();
        if (resourceId != null) {
            dbService.rxSearch(providerByQuery, false, "")
                    .flatMapCompletable(result -> {
                        JsonArray esHits = result.getJsonArray("hits");
                        if (esHits != null && esHits.size() == 0) {
                            return Completable.error(new FileNotFoundThrowable("The requested files are not found"));
                        }
                        getValue(resourceId)
                                .map(file -> {
                                    logger.debug("file name on cache = " + file);
                                    boolean doesFileExists = Files.exists(Path.of(file));
                                    String downloadLink = "";
                                    if (doesFileExists) {
                                        downloadLink = "https://" + System.getenv("SERVER_NAME") + "/provider/public/"
                                                + file.substring(36);
                                        didResponseEnded.set(true);
                                        response.setStatusCode(ACCEPTED)
                                                .putHeader("content-type", "text/plain")
                                                .setStatusMessage("Please kindly wait as your download links are getting ready-single")
                                                .end("Please check your email for the links soon." + "\n"
                                                        + "Note: The time frame for the email is subjected to the number of files to zip.");
                                        emailJob(email, null, null);
                                    }
                                    return didResponseEnded.get();
                                }).flatMapCompletable(didResponseEnd -> {
                                    logger.debug("did response end = " + didResponseEnd);
                                    logger.debug("did response got ended = " + response.ended());
                                    if (!didResponseEnd) {
                                        SchedulerFactory stdSchedulerFactory = new StdSchedulerFactory();
                                        Scheduler scheduler = stdSchedulerFactory.getScheduler();
                                        scheduler.start();

                                        logger.debug("Is scheduler started: " + scheduler.isStarted());
                                        JobDataMap jobDataMap = new JobDataMap();
                                        jobDataMap.put("uuid", uuid);
                                        jobDataMap.put("finalHitsSize", esHits.size());
                                        jobDataMap.put("resourceId", resourceId);
                                        jobDataMap.put("distinctIds", null);
                                        jobDataMap.put("email", email);

                                        // define the job and tie it to our JobScheduler class
                                        JobDetail job = JobBuilder.newJob(ProviderScheduler.class)
                                                .withIdentity(String.valueOf(uuid), "provider")
                                                .usingJobData(jobDataMap)
                                                .build();
                                        logger.debug("Job key: " + job.getKey());

                                        // Trigger the job to run now
                                        Trigger trigger = newTrigger()
                                                .withIdentity(String.valueOf(uuid), "provider")
                                                .startNow()
                                                .withSchedule(simpleSchedule())
                                                .build();
                                        logger.debug("trigger key: " + trigger.getKey());

                                        scheduler.getListenerManager().addJobListener(
                                                new JobSchedulerListener());

                                        // Tell quartz to schedule the job using our trigger
                                        boolean isSchedulerExceptionCaught = false;
                                        try {
                                            scheduler.scheduleJob(job, trigger);
                                        } catch (SchedulerException e) {
                                            isSchedulerExceptionCaught = true;
                                            logger.debug("Scheduler exception caused due to: " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                        if (isSchedulerExceptionCaught) {
                                            logger.debug("inside scheduler exception caught");
                                            return Completable.error(new SchedulerException("Scheduler exception caught"));
                                        }
                                    }
                                    return Completable.complete();
                                }).subscribe(() -> {
                                    if (!didResponseEnded.get()) {
                                        response.setStatusCode(ACCEPTED)
                                                .putHeader("content-type", "text/plain")
                                                .setStatusMessage("Please kindly wait as your download links are getting ready")
                                                .end("Please check your email for the links soon." + "\n"
                                                        + "Note: The time frame for the email is subjected to the number of files to zip.");
                                    }
                                    }, throwable -> apiFailure(context, throwable));
                        return Completable.complete();
                    }).subscribe(() -> { }, throwable -> apiFailure(context, throwable));
        } else {
            dbService.rxSearch(providerByQuery, true, "60m")
                    .flatMap(result -> {
                        int total = result.getInteger("total");
                        logger.debug("total no = "+ total);

                        AtomicInteger totalNoOfTimesToInvokeScrollApi = new AtomicInteger();
                        if ((total % size) == 0) {
                            totalNoOfTimesToInvokeScrollApi.set((total / size) - 1);
                        } else {
                            totalNoOfTimesToInvokeScrollApi.set((total / size)) ;
                        }
                        noOfScrollCalls.set(totalNoOfTimesToInvokeScrollApi.get());
                        logger.debug("totalNoOfTimesToInvokeScrollApi = "+ totalNoOfTimesToInvokeScrollApi);

                        hits.set(result.getJsonArray("hits"));
                        if (hits.get().size() == 0) {
                            return Single.error(new FileNotFoundThrowable("The requested files are not found"));
                        }
                        String id;
                        for (Object hit : hits.get()) {
                            if (hit instanceof JsonObject) {
                                id = ((JsonObject) hit).getString("id");
                                if (id.endsWith(".public")) {
                                    listOfEligibleIds.add(id);
                                    //add files also
                                    String fileName = ((JsonObject) hit).getJsonObject("data").getString("filename");
                                    String fileDirectory = PROVIDER_PATH + "public/" + id + "/" + fileName;
                                    listOfFilesNeedToBeZipped.add(new File(fileDirectory));
                                }
                            }
                        }

                        List<String> itemList1 = new ArrayList<>();
                        Single<JsonObject> scrolledSearchResult =
                                dbService.rxScrolledSearch(result.getString("scroll_id"),
                                        "60m", null, null);
                        scrolledSearchResult.flatMap(scrolledResult-> {
                            if (totalNoOfTimesToInvokeScrollApi.get() > 0) {
                                itemList1.add("visited");
                            }
//                            logger.debug("scrolledResult = " + scrolledResult.encodePrettily());

                            for (int i = 0; i < scrolledResult.getJsonArray("hits").size(); i++) {
                                if (scrolledResult.getJsonArray("hits").size() > 0
                                        && scrolledResult.getJsonArray("hits") != null
                                        && scrolledResult.getJsonArray("hits").getJsonObject(i) != null) {
                                    String idFromScrolledSearch = scrolledResult.getJsonArray("hits").getJsonObject(i).getString("id");
                                    logger.debug("idFromScrolledSearch = " + idFromScrolledSearch);
                                    if (idFromScrolledSearch.endsWith(".public")) {
                                        listOfEligibleIds.add(idFromScrolledSearch);
                                        //add files also
                                        String fileName = scrolledResult.getJsonArray("hits").getJsonObject(i).getJsonObject("data").getString("filename");
                                        String fileDirectory = PROVIDER_PATH + "public/" + idFromScrolledSearch + "/" + fileName;
                                        listOfFilesNeedToBeZipped.add(new File(fileDirectory));
                                    }
                                }
                            }

                            return Single.just(listOfFilesNeedToBeZipped);
                        }).repeatUntil(() -> totalNoOfTimesToInvokeScrollApi.getAndDecrement() < 2).flatMapCompletable(res -> {
                            logger.debug("item list 1 size after repeat = "+ itemList1.size());
                            if (itemList1.size() == noOfScrollCalls.get()) {
                                if (listOfEligibleIds.size() == 0) {
                                    return Completable.error(new
                                            UnauthorisedThrowable("This is used only for public datasets and not secure ones"));
                                }
                                distinctIds.set(listOfEligibleIds.stream().distinct().collect(Collectors.toList()));
                                logger.debug("distinct Ids= " + distinctIds.get().toString());

                                List<String> itemList = new ArrayList<>();
                                Map<String, Long> downloadLinksMap = new HashMap<>();

                                for (int i = 0; i < distinctIds.get().size(); i++) {
                                    int finalI = i;
                                    getValue(distinctIds.get().get(i))
                                            .flatMap(file -> {
                                                itemList.add("visited");
                                                logger.debug("Item list size =" + itemList.size());
                                                String zipLink;
                                                logger.debug("result for multiple ids = " + file);
                                                boolean doesFileExists = Files.exists(Path.of(file));
                                                logger.debug("doesFileExists for multiple ids= " + doesFileExists);
                                                int noOfFilesInZip = 0;
                                                int noOfFilesInResourceId = 0;
                                                if (!"absent".equals(file)) {
                                                    noOfFilesInZip = new ZipFile(file).getFileHeaders()
                                                            .stream().distinct().collect(Collectors.toList()).size();
                                                    noOfFilesInResourceId = Objects.requireNonNull(
                                                            new File("/api-server/webroot/provider/public/" +
                                                                    distinctIds.get().get(finalI)).listFiles()).length;
                                                    logger.debug("noOfFilesInZip of = " + file + "-->" + noOfFilesInZip);
                                                    logger.debug("noOfFilesInResourceId of = " +
                                                            distinctIds.get().get(finalI) + "-->" + noOfFilesInResourceId);
                                                }

                                                //find a way to add only listOfFilesNeedToBeAddedToZipFile to zip file instead of all files
//                                                listOfFilesNeedToBeZipped.addAll(listOfFilesNeedToBeAddedToZipFile);
                                                if (doesFileExists && !(noOfFilesInResourceId > noOfFilesInZip)) {
                                                    zipLink = "https://" + System.getenv("SERVER_NAME") + "/provider/public/"
                                                            + file.substring(36);
                                                    finalZipLinks.add(zipLink);
                                                    long fileSize = Files.size(Path.of(file));
                                                    logger.debug("file size in bytes = " + fileSize);
                                                    downloadLinksMap.put(zipLink, fileSize / 1073741824);
                                                } else {
                                                    listOfIdsNeedToBeSentToScheduler.add(distinctIds.get().get(finalI));
                                                }

                                                finalListOfIdsNeedToBeSentToScheduler.set(listOfIdsNeedToBeSentToScheduler.stream().distinct().collect(Collectors.toList()));
                                                logger.debug("finalListOfIdsNeedToBeSentToScheduler = " + finalListOfIdsNeedToBeSentToScheduler);
                                                logger.debug("finalZipLinks of consumer =" + finalZipLinks);

                                                if (itemList.size() == distinctIds.get().size()
                                                        && finalZipLinks.size() == distinctIds.get().size()
                                                        && !(noOfFilesInResourceId > noOfFilesInZip)) {
                                                    String sub_category = subCategoryEntryField.getString("sub_category");
                                                    emailJob(email, downloadLinksMap, sub_category);
                                                    didResponseEnded.set(true);
                                                    response.setStatusCode(ACCEPTED)
                                                            .putHeader("content-type", "text/plain")
                                                            .setStatusMessage("Please kindly wait as your download links are getting ready-multiple")
                                                            .end("Thanks for your interest in the <" + sub_category + "> corpus. \n" +
                                                                    "Your request for download has been received. Soon, you will receive an email from <DataSetu Team, patzzziejordan@gmail.com> " +
                                                                    "to the respective email-id which will contain downloadable links for the same." + "\n\n"
                                                                    + "Note: The time frame for the email is subjected to the number of files to zip.");
                                                    return Single.never();
                                                }
                                                return Single.just(didResponseEnded.get());
                                            }).flatMapCompletable(didResponseEnd -> {
                                                logger.debug("did response end = " + didResponseEnd);
                                                logger.debug("Item list size =" + itemList.size());

                                                if (!didResponseEnd && itemList.size() == distinctIds.get().size()) {
                                                    SchedulerFactory stdSchedulerFactory = new StdSchedulerFactory();
                                                    Scheduler scheduler = stdSchedulerFactory.getScheduler();
                                                    scheduler.start();

                                                    logger.debug("distinct ids need to be sent for zip =" + listOfIdsNeedToBeSentToScheduler);
                                                    logger.debug("Is scheduler started: " + scheduler.isStarted());
                                                    String sub_category = subCategoryEntryField.getString("sub_category");
                                                    JobDataMap jobDataMap = new JobDataMap();
                                                    jobDataMap.put("uuid", uuid);
                                                    jobDataMap.put("finalHitsSize", total);
                                                    jobDataMap.put("resourceId", null);
                                                    jobDataMap.put("distinctIds", finalListOfIdsNeedToBeSentToScheduler.get());
                                                    jobDataMap.put("listOfFilesNeedToBeZipped", listOfFilesNeedToBeZipped);
                                                    jobDataMap.put("email", email);
                                                    jobDataMap.put("finalZipLinks", finalZipLinks);
                                                    jobDataMap.put("sub_category", sub_category);

                                                    // define the job and tie it to our JobScheduler class
                                                    JobDetail job = JobBuilder.newJob(ProviderScheduler.class)
                                                            .withIdentity(String.valueOf(uuid), "provider")
                                                            .usingJobData(jobDataMap)
                                                            .build();
                                                    logger.debug("Job key: " + job.getKey());

                                                    // Trigger the job to run now
                                                    Trigger trigger = newTrigger()
                                                            .withIdentity(String.valueOf(uuid), "provider")
                                                            .startNow()
                                                            .withSchedule(simpleSchedule())
                                                            .build();
                                                    logger.debug("trigger key: " + trigger.getKey());

                                                    scheduler.getListenerManager().addJobListener(
                                                            new JobSchedulerListener());

                                                    // Tell quartz to schedule the job using our trigger
                                                    boolean isSchedulerExceptionCaught = false;
                                                    try {
                                                        scheduler.scheduleJob(job, trigger);
                                                    } catch (SchedulerException e) {
                                                        isSchedulerExceptionCaught = true;
                                                        logger.debug("Scheduler exception caused due to: " + e.getMessage());
                                                        e.printStackTrace();
                                                    }
                                                    if (isSchedulerExceptionCaught) {
                                                        logger.debug("inside scheduler exception caught");
                                                        return Completable.error(new SchedulerException("Scheduler exception caught"));
                                                    }
                                                    return Completable.complete();
                                                }
                                                return Completable.never();
                                            }).subscribe(() -> {
                                                logger.debug("List size =" + itemList.size());
                                                if (!didResponseEnded.get() && itemList.size() == distinctIds.get().size()) {
                                                    String sub_category = subCategoryEntryField.getString("sub_category");
                                                    response.setStatusCode(ACCEPTED)
                                                            .putHeader("content-type", "text/plain")
                                                            .setStatusMessage("Please kindly wait as your download links are getting ready")
                                                            .end("Thanks for your interest in the <" + sub_category + "> corpus. \n" +
                                                                    "Your request for download has been received. Soon, you will receive an email from <DataSetu Team, patzzziejordan@gmail.com> " +
                                                                    "to the respective email-id which will contain downloadable links for the same." + "\n\n"
                                                                    + "Note: The time frame for the email is subjected to the number of files to zip.");
                                                }
                                            }, throwable -> apiFailure(context, throwable));
                                        }
                                    }
                                    return Completable.complete();
                                }).subscribe(()-> { }, throwable -> apiFailure(context, throwable));
                        return Single.just("");
                    }).subscribe(s-> { }, throwable -> apiFailure(context, throwable));
        }
    }

    public void downloadByQuery(RoutingContext context)  {
        logger.debug("In download by query");

        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        MultiMap parameters = request.params();
        String token = request.getParam("token");
        String resourceId = request.getParam("id");

        logger.debug("Parameters from request: " + parameters);

        logger.debug("token=" + token);
        logger.debug("resource Id=" + resourceId);
        if (token == null) {
            apiFailure(context, new BadRequestThrowable("No access token/resource Id found in request"));
            return;
        }

        if (!isValidToken(token)) {
            apiFailure(context, new UnauthorisedThrowable("Malformed access token"));
            return;
        }

        if(resourceId != null && !isValidResourceID(resourceId)) {
            apiFailure(context, new UnauthorisedThrowable("Invalid Id"));
            return;
        }

        CONSUMER_PATH = "/consumer/" + token + "/";

        Queries query = new Queries();

        int size = 10000; //max set of results per search request
        JsonObject downloadByQuery = query.getDownloadByQuery();
        JsonArray jsonArray = downloadByQuery.put("size", size).getJsonObject("query").getJsonObject("bool")
                .getJsonObject("must").getJsonObject("bool").getJsonArray("should");

        JsonObject filterByAuthorisedIds = downloadByQuery.getJsonObject("query").getJsonObject("bool");
        if (resourceId != null) {
            filterByAuthorisedIds.put("filter",
                    new JsonObject().put("terms",
                            new JsonObject().put("id.keyword",
                                    new JsonArray().add(resourceId))));
        }

        List<Map.Entry<String, String>> entries = parameters.entries();
        for (int i=0; i<entries.size(); i++) {
            String key = "";
            String value = "";
            if(!entries.get(i).getKey().equalsIgnoreCase("token")
                    && !"id".equalsIgnoreCase(entries.get(i).getKey())) {
                key = entries.get(i).getKey();
                value = entries.get(i).getValue();
                logger.debug("key: " + key);
                logger.debug("value: " + value);
                jsonArray.add(new JsonObject().put("match", new JsonObject().put("data.metadata."+key, value)));
            }

            if("category".equalsIgnoreCase(entries.get(i).getKey())) {
                jsonArray.add(new JsonObject().put("match", new JsonObject().put(key, value)));
            }
        }

        if(jsonArray.size() == 0) {
            apiFailure(context, new BadRequestThrowable("Please provide the query parameters to download the files"));
            return;
        }

        UUID uuid = UUID.randomUUID();
        logger.debug("uuid: " + uuid);
        Map<String, String> emailDetails = new HashMap<>();
//        final String finalDownloadLink = "https://" +System.getenv("SERVER_NAME") +CONSUMER_PATH + uuid;  //Could be used in future if needed
        isTokenExpired(token).subscribe();
        setConsumerEmailDetails(token, emailDetails).subscribe();
        checkAuthorisation(token, READ_SCOPE).flatMap(id -> {
            logger.debug("authorised ids of consumer=" + id);
            JsonArray authorisedResourceIds = new JsonArray();
            Iterator<Object> iterator = id.stream().iterator();
            while (iterator.hasNext() && resourceId == null) {
                String next = (String) iterator.next();
                filterByAuthorisedIds.put("filter",
                        new JsonObject().put("terms",
                                new JsonObject().put("id.keyword",
                                        authorisedResourceIds.add(next))));
            }

            logger.debug("constructed query for download by query API for category/subCategory is: "
                    + downloadByQuery.toString());
            return dbService.rxSecureSearch(downloadByQuery, token, false, "");

        }).flatMapCompletable(result-> {
            logger.debug("Response from search endpoint:" + result.toString());
            JsonArray hits = result.getJsonArray("hits");

            logger.debug("tokenExpiredDetails=" + tokenExpiredDetails.toString());
            if (tokenExpiredDetails.get("tokenExpiredDetails")) {
                return Completable.error(new UnauthorisedThrowable("Unauthorised due to token expiry"));
            }

            String email = emailDetails.get("email");
            logger.debug("consumerEmail=" + email);
            if (email == null || "".equals(email)) {
                return Completable.error(new UnauthorisedThrowable("Email is missing"));
            }

            if (hits.size() == 0) {
                return Completable.error(new FileNotFoundThrowable("The requested files are not found"));
            }
            invokeScheduler(token, uuid, emailDetails, hits);
            return Completable.complete();
        }).subscribe(()-> response.setStatusCode(ACCEPTED)
                        .setStatusMessage("Please kindly wait as your download links are getting ready")
                        .end("Please check your email to find the download links..!!" + "\n"),
                throwable -> apiFailure(context, throwable));

    }

    private Completable invokeScheduler(String token, UUID uuid, Map<String, String> emailDetails, JsonArray hits) throws SchedulerException {
        SchedulerFactory stdSchedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = stdSchedulerFactory.getScheduler();
        scheduler.start();

        logger.debug("Is scheduler started: " + scheduler.isStarted());
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("token", token);
        jobDataMap.put("uuid", uuid);
        jobDataMap.put("finalHits", hits);
        jobDataMap.put("email", emailDetails.get("email"));

        // define the job and tie it to our JobScheduler class
        JobDetail job = JobBuilder.newJob(JobScheduler.class)
                .withIdentity(String.valueOf(uuid), "download")
                .usingJobData(jobDataMap)
                .build();
        logger.debug("Job key: " + job.getKey());

        // Trigger the job to run now
        Trigger trigger = newTrigger()
                .withIdentity(String.valueOf(uuid), "download")
                .startNow()
                .withSchedule(simpleSchedule())
                .build();
        logger.debug("trigger key: " + trigger.getKey());

        scheduler.getListenerManager().addJobListener(
                new JobSchedulerListener());

        // Tell quartz to schedule the job using our trigger
        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.debug("Scheduler exception caused due to: " + e.getMessage());
            e.printStackTrace();
            return Completable.error(new SchedulerException("Scheduler exception caught"));
        }
        return Completable.complete();
    }

    // TODO: Handle server token
    // TODO: Handle regexes
    // Method that makes the HTTPS request to the auth server
    public Completable introspect(String token, List<FileUpload> fileUploads) {
        logger.debug("In introspect");
        JsonObject body = new JsonObject();
        body.put("token", token);

        WebClientOptions webClientOptions = new WebClientOptions()
                .setSsl(true)
                .setKeyStoreOptions(new JksOptions().setPath(AUTH_TLS_CERT_PATH).setPassword(AUTH_TLS_CERT_PASSWORD));

        if ("auth.local".equalsIgnoreCase(AUTH_SERVER)) {
            webClientOptions.setTrustAll(true);
        }

        WebClient client = WebClient.create(vertx, webClientOptions);

        return client.post(443, AUTH_SERVER, INTROSPECT_ENDPOINT)
                .ssl(true)
                .putHeader("content-type", "application/json")
                .rxSendJsonObject(body)
                .flatMapMaybe(response -> {
                    if (response.statusCode() == 200) {
                        return Maybe.just(response.bodyAsString());
                    } else {
                        logger.debug("Auth response=" + response.bodyAsString());
                        return Maybe.empty();
                    }
                })
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapCompletable(data -> {
                    if (data.isPresent()) {
                        return setValue(token, data.get());
                    } else {
                        if (fileUploads != null) {
                            deleteUploads(fileUploads);
                        }
                        return Completable.error(new UnauthorisedThrowable("Unauthorised"));
                    }
                });
    }

    // Method that uses the redis cache for authorising requests.
    // Uses introspect if needed
    public Completable checkAuthorisation(String token, String scope, JsonArray requestedIds,
                                          List<FileUpload> fileUploads) {

        Set<String> requestedSet = IntStream.range(0, requestedIds.size())
                .mapToObj(requestedIds::getString)
                .collect(Collectors.toCollection(HashSet::new));

        return getValue(token)
                .flatMapCompletable(
                        cache -> "absent".equalsIgnoreCase(cache) ? introspect(token, fileUploads) : Completable.complete())
                // TODO: Avoid reading from cache again
                .andThen(Single.defer(() -> getValue(token)))
                .flatMapMaybe(result -> "absent".equalsIgnoreCase(result)
                        ? Maybe.empty()
                        : Maybe.just(new JsonObject(result).getJsonArray("request")))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapMaybe(resultArray -> resultArray
                        .map(authorisedIds ->
                                // TODO: In this case check for methods,
                                // body, API etc
                                Maybe.just(IntStream.range(0, authorisedIds.size())
                                        .filter(i -> authorisedIds
                                                .getJsonObject(i)
                                                .getJsonArray("scopes")
                                                .contains(scope))
                                        .mapToObj(i ->
                                                authorisedIds.getJsonObject(i).getString("id"))
                                        .collect(Collectors.toCollection(HashSet::new))))
                        .orElseGet(Maybe::empty))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapCompletable(hashSet -> hashSet.map(authorisedSet -> {
                            logger.debug("Authorized set of consumer ResourceId's: " + authorisedSet.toString());
                            logger.debug("Requested set of consumer ResourceId's: " + requestedSet.toString());
                            if (authorisedSet.containsAll(requestedSet)) {
                                // Straightforward case. No need to worry about nested IDs
                                return Completable.complete();
                            } else {
                                // This is the case when the resource ID in the auth policy is a coarse one that
                                // subsumes nested resource IDs

                                int authorisedCount = 0;

                                for (String requestedResourceID : requestedSet) {
                                    boolean found = false;

                                    for (String authorisedResourceID : authorisedSet) {
                                        if (requestedResourceID.contains(authorisedResourceID)) {
                                            authorisedCount++;
                                            found = true;
                                        }
                                    }

                                    if (!found) {
                                        // Requested resource ID has not been found in any of the authorised IDs in the
                                        // auth policy
                                        if (fileUploads != null) {
                                            deleteUploads(fileUploads);
                                        }
                                        return Completable.error(new UnauthorisedThrowable("ACL does not match"));
                                    }
                                }

                                if (authorisedCount == requestedSet.size()) {
                                    return Completable.complete();
                                } else {
                                    if (fileUploads != null) {
                                        deleteUploads(fileUploads);
                                    }
                                    return Completable.error(new UnauthorisedThrowable("ACL does not match"));
                                }
                            }
                        })
                        .orElseGet(() -> {
                            if (fileUploads != null) {
                                deleteUploads(fileUploads);
                            }
                            return Completable.error(new UnauthorisedThrowable("Unauthorised"));
                        }));
        //                .flatMapCompletable(hashSet -> hashSet.map(authorisedSet ->
        // (authorisedSet.containsAll(requestedSet)
        //                        ? Completable.complete()
        //                        : Completable.error(new UnauthorisedThrowable("ACL does not match"))))
        //                        .orElseGet(() -> Completable.error(new UnauthorisedThrowable("Unauthorised"))));
    }

    public Single<JsonArray> checkAuthorisation(String token, String scope) {

        logger.debug("In check authorisation");

        return getValue(token)
                .flatMapCompletable(
                        cache -> "absent".equalsIgnoreCase(cache) ? introspect(token, null) : Completable.complete())
                // TODO: Avoid reading from cache again
                .andThen(Single.defer(() -> getValue(token)))
                .flatMapMaybe(result -> "absent".equalsIgnoreCase(result)
                        ? Maybe.empty()
                        : Maybe.just(new JsonObject(result).getJsonArray("request")))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMapMaybe(resultArray -> resultArray
                        .map(authorisedIds ->
                                // TODO: In this case check for methods, body, API etc
                                Maybe.just(new JsonArray(IntStream.range(0, authorisedIds.size())
                                        .filter(i -> authorisedIds
                                                .getJsonObject(i)
                                                .getJsonArray("scopes")
                                                .contains(scope))
                                        .mapToObj(i ->
                                                authorisedIds.getJsonObject(i).getString("id"))
                                        .collect(Collectors.toList()))))
                        .orElseGet(Maybe::empty))
                .map(Optional::of)
                .toSingle(Optional.empty())
                .flatMap(result -> result.map(Single::just)
                        .orElseGet(() -> Single.error(new UnauthorisedThrowable("Unauthorised"))));
    }

    private void apiFailure(RoutingContext context, Throwable t) {
        logger.debug("In apifailure");
        logger.debug("Message=" + t.getMessage());
        if (t instanceof BadRequestThrowable) {
            logger.debug("In bad request");
            context.response()
                    .setStatusCode(BAD_REQUEST)
                    .putHeader("content-type", "application/json")
                    .end(t.getMessage());
        } else if (t instanceof UnauthorisedThrowable) {
            logger.debug("In unauthorised");
            context.response()
                    .setStatusCode(FORBIDDEN)
                    .putHeader("content-type", "application/json")
                    .end(t.getMessage());
        } else if (t instanceof FileNotFoundThrowable) {
            logger.debug("In FileNotFoundThrowable");
            context.response()
                    .setStatusCode(404)
                    .putHeader("content-type", "application/json")
                    .end(t.getMessage());

        } else if (t instanceof FileNotFoundException) {
        logger.debug("In FileNotFound");
        context.response()
                .setStatusCode(NOT_FOUND)
                .putHeader("content-type", "application/json")
                .end(t.getMessage());

        } else if (t instanceof ServiceException) {

            logger.debug("Service exception");
            ServiceException serviceException = (ServiceException) t;

            if (serviceException.failureCode() == 400 || serviceException.failureCode() == 404) {
                context.response()
                        .setStatusCode(BAD_REQUEST)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status", "error")
                                .put("message", serviceException.getMessage())
                                .encode());
            } else {
                context.response()
                        .setStatusCode(INTERNAL_SERVER_ERROR)
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject()
                                .put("status", "error")
                                .put("message", serviceException.getMessage())
                                .encode());
            }

        } else {
            logger.debug("In internal error");
            context.response()
                    .setStatusCode(INTERNAL_SERVER_ERROR)
                    .putHeader("content-type", "application/json")
                    .end(t.getMessage());
        }

    }

    private String commonPrefix(JsonArray resourceIds) {

        // Getting length of shortest resourceId
        int minLength = resourceIds.stream()
                .map(Object::toString)
                .mapToInt(String::length)
                .min()
                .orElse(0);

        String commonPrefix = "";
        char current;

        for (int i = 0; i < minLength; i++) {

            // using reference character from 1st string to match
            current = resourceIds.getString(0).charAt(i);

            for (int j = 1; j < resourceIds.size(); j++) {

                // If the i th character is not same in all the string simply return the commonPrefix till last
                // character.
                if (resourceIds.getString(j).charAt(i) != current) {
                    return commonPrefix;
                }
            }

            // else till i th character all strings are same.
            commonPrefix += current;
        }
        return commonPrefix;
    }

    private boolean isValidResourceID(String resourceID) {

        logger.debug("In isValidResourceId");
        logger.debug("Received resource id = " + resourceID);
        // TODO: Handle sub-categories correctly
        // String validRegex = "[a-z_.\\-]+\\/[a-f0-9]{40}\\/[a-z_.\\-]+\\/[a-zA-Z0-9_.\\-]+\\/[a-zA-Z0-9_.\\-]+";
        String validRegex = "[a-z_.\\-]+\\/[a-f0-9]{40}\\/[a-z_.\\-]+(\\/[a-zA-Z0-9_.\\-]+){2,7}";

        return resourceID.matches(validRegex);
    }

    //    private boolean isValidScrollID(String scrollID) {
    //
    //        logger.debug("In isValidScrollId");
    //        logger.debug("Received Scroll id = " + scrollID);
    //
    //        String validRegex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
    //        return scrollID.matches(validRegex);
    //    }

    private boolean isValidToken(String token) {

        logger.debug("In isValidToken");
        logger.debug("Received token = " + token);

        String validRegex = "^(auth.local|auth.datasetu.org)\\/[a-f0-9]{32}";
        return token.matches(validRegex);
    }

    private void deleteUploads(List<FileUpload> fileUploads) {
        fileUploads.forEach(v -> {
            try {
                Files.deleteIfExists(Paths.get(v.uploadedFileName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private Single<String> setConsumerEmailDetails(String token, Map<String, String> emailDetails) {
        logger.debug("In consumerEmail details");
        return getValue(token)
                .map(result -> {
                    if("absent".equalsIgnoreCase(result)) {
                        logger.debug("email is empty");
                        return "";
                    } else {
                        String consumerEmail = new JsonObject(result).getString("consumer");
                        logger.debug("Email from introspect="  + consumerEmail);
                        emailDetails.put("email", consumerEmail);
                        return consumerEmail;
                    }
                });
    }

    private void emailJob(String email, Map<String, Long> downloadLinksMap, String sub_category) {

        logger.debug("In email Job");
        logger.debug("Recipient email= " + email);
        String link;
        long size;
        String message = "Dear consumer,"
                + "\n\n" + "The downloadable links for the datasets <" + sub_category + "> you requested are ready to be served. Please use below link to download the datasets as a zip file.";
        StringBuilder downloadLinkMessage = new StringBuilder();

        Set<String> keySet = downloadLinksMap.keySet();
        for(String key: keySet) {
            link = key;
            size = downloadLinksMap.get(key);
            downloadLinkMessage.append(link).append(" ").append("(").append(size).append("Gb)").append("\n");
        }
        logger.debug("downloadLinkMessage =" + downloadLinkMessage);
        String note = "Note: These links will be made available only for five days from the time of initial request made for zip."
                + "\n" + "Post which, the files will be deleted. So, act accordingly.";
        String finalMessageToConsumer = message + "\n" + downloadLinkMessage + "\n" + note + "\n\n" + "Thanks," + "\n" + "Datsetu";

        Properties properties = new Properties();
        properties.put("mail.smtp.host", System.getenv("HOST")); //host name
        properties.put("mail.smtp.port", System.getenv("EMAIL_PORT")); //TLS port
        properties.put("mail.debug", "false"); //enable when you want to see mail logs
        properties.put("mail.smtp.auth", "true"); //enable auth
        properties.put("mail.smtp.starttls.enable", "true"); //enable starttls
        properties.put("mail.smtp.ssl.trust", System.getenv("HOST")); //trust this host
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2"); //specify secure protocol
        final String username = "patzzziejordan@gmail.com";
        final String password = System.getenv("EMAIL_PWD");
        logger.debug("password = " + password);
        logger.debug("host = " + System.getenv("HOST"));
        logger.debug("port = " +  System.getenv("EMAIL_PORT"));
        try{
            Session session = Session.getInstance(properties,
                    new Authenticator(){
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }});

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(username));
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress(email));
            msg.setSubject("Download links");
            msg.setText(finalMessageToConsumer);
            logger.debug("sending email");
            Transport.send(msg);
            logger.debug("sent email successfully with below details: " + "\n" + msg.getContent().toString() + "\n" + Arrays.toString(msg.getAllRecipients()));
        } catch (AddressException | IOException e) {
            e.printStackTrace();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        logger.debug("email job done");
    }
    public boolean isEmailValid(String email) {
        Pattern EMAIL_REGEX = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
        return EMAIL_REGEX.matcher(email).matches();
    }

}
