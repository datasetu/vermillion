package vermillion.util;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.FileUpload;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import io.vertx.serviceproxy.ServiceException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.validator.GenericValidator;
import vermillion.broker.reactivex.BrokerService;
import vermillion.database.Queries;
import vermillion.database.reactivex.DbService;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import org.joda.time.format.ISODateTimeFormat;

public class Utils{

    public final Logger logger = LoggerFactory.getLogger(Utils.class);

    // HTTP Codes
    public final int OK = 200;
    public final int CREATED = 201;
    public final int ACCEPTED = 202;
    public final int BAD_REQUEST = 400;
    public final int FORBIDDEN = 403;
    public final int INTERNAL_SERVER_ERROR = 500;

    public void apiFailure(RoutingContext context, Throwable t) {
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
            }
            else
            {
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

    public String commonPrefix(JsonArray resourceIds) {

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

    public boolean isValidResourceID(String resourceID) {

        logger.debug("In isValidResourceId");
        logger.debug("Received resource id = " + resourceID);
        // TODO: Handle sub-categories correctly
        String validRegex = "[a-z_.\\-]+\\/[a-f0-9]{40}\\/[a-z_.\\-]+\\/[a-zA-Z0-9_.\\-]+\\/[a-zA-Z0-9_.\\-]+";

        return resourceID.matches(validRegex);
    }

//    public boolean isValidScrollID(String scrollID) {
//
//        logger.debug("In isValidScrollId");
//        logger.debug("Received Scroll id = " + scrollID);
//
//        String validRegex = "^([A-Za-z0-9+/]{4})*([A-Za-z0-9+/]{3}=|[A-Za-z0-9+/]{2}==)?$";
//        return scrollID.matches(validRegex);
//    }

    public boolean isValidToken(String token) {

        logger.debug("In isValidToken");
        logger.debug("Received token = " + token);

        String validRegex = "^(auth.local|auth.datasetu.org)\\/[a-f0-9]{32}";
        return token.matches(validRegex);
    }

    public void deleteUploads(HashMap<String, FileUpload> fileUploads){
        fileUploads.forEach((k, v) -> {
            try {
                Files.deleteIfExists(Paths.get(v.uploadedFileName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}