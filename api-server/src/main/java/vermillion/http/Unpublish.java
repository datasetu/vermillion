package vermillion.http;

import io.reactivex.Completable;
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
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.IntStream;

public class Unpublish extends HttpServerVerticle {

    public final Logger logger = LoggerFactory.getLogger(Unpublish.class);

    // Auth server constants
    public final String WEBROOT = "webroot/";
    public final String PROVIDER_PATH = "/api-server/webroot/provider/";
    public final String WRITE_SCOPE = "write";

    public Utils utils = new Utils();

    public void unpublish(RoutingContext context) {

        logger.debug("In unpublish API");
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        JsonObject requestBody = null;
        String resourceId;
        String token;
        JsonArray files;

        try {
            requestBody = context.getBodyAsJson();
        } catch (Exception e) {
            utils.apiFailure(context, new BadRequestThrowable("Body is not a valid JSON"));
            return;
        }

        if (requestBody == null) {
            utils.apiFailure(context, new BadRequestThrowable("Body is null"));
            return;
        }

        Set<String> permittedFieldSet = new HashSet<>();
        permittedFieldSet.add("files");
        permittedFieldSet.add("id");
        permittedFieldSet.add("token");

        if (!requestBody.containsKey("id")) {
            utils.apiFailure(context, new BadRequestThrowable("No id found in body"));
            return;
        }

        if (!requestBody.containsKey("token")) {
            utils.apiFailure(context, new BadRequestThrowable("No token in body"));
            return;
        }

        if (!requestBody.containsKey("files")) {
            utils.apiFailure(context, new BadRequestThrowable("No files in body"));
            return;
        }

        if (!permittedFieldSet.containsAll(requestBody.fieldNames())) {
            utils.apiFailure(context, new BadRequestThrowable("Body contains unnecessary fields"));
            return;
        }

        Object idObj = requestBody.getValue("id");

        if (!(idObj instanceof String))
        {
            utils.apiFailure(context, new BadRequestThrowable("ID is not valid"));
            return;
        }

        Object tokenObj = requestBody.getValue("token");

        if (!(tokenObj instanceof String))
        {
            utils.apiFailure(context, new BadRequestThrowable("Token is not valid"));
            return;
        }

        Object filesObj = requestBody.getValue("files");

        if (!(filesObj instanceof JsonArray)){
            utils.apiFailure(context, new BadRequestThrowable("files is not valid"));
            return;
        }

        resourceId = requestBody.getString("id").trim();
        token = requestBody.getString("token").trim();
        files = requestBody.getJsonArray("files");

        if ("".equals(resourceId) || resourceId == null) {
            utils.apiFailure(context, new BadRequestThrowable("No resource ID found in request"));
            return;
        }

        if ("".equals(token) || token == null) {
            utils.apiFailure(context, new BadRequestThrowable("No access token found in request"));
            return;
        }

        if (!utils.isValidToken(token) || !utils.isValidResourceID(resourceId)) {
            utils.apiFailure(context, new UnauthorisedThrowable("Malformed resource ID or token"));
            return;
        }

        logger.debug("files=" + files.encodePrettily());

        logger.debug("files size = " + files.size());

        if (files.size() == 0) {
            utils.apiFailure(context, new BadRequestThrowable("Invalid files"));
            return;
        }

        for (int i = 0; i < files.size(); i++) {
            if (!(files.getValue(i) instanceof String)) {
                utils.apiFailure(context, new BadRequestThrowable("files are not valid String"));
                return;
            }
        }

        JsonArray requestedIdList = new JsonArray().add(resourceId);

        Queries queries = new Queries();

        JsonObject baseQuery = queries.getBaseQuery();
        JsonArray filterQuery = queries.getFilterQuery();
        JsonObject termQuery = queries.getTermQuery();
        JsonObject filesQuery = queries.getFilesQuery();
        checkAuthorisation(token, WRITE_SCOPE, requestedIdList)
                .andThen(Completable.defer(() -> {
                    JsonArray finalFiles = new JsonArray();
                    for (int i = 0; i < files.size(); i++) {
                        String accessFolder = PROVIDER_PATH + (resourceId.endsWith(".public") ? "public/" : "secure/");

                        String providerDirStructure = accessFolder + resourceId;
                        logger.debug("Provider dir structure=" + providerDirStructure);

                        String providerFilePath = accessFolder + resourceId + "/" + files.getString(i);
                        try {
                            if (Files.exists(Paths.get(providerFilePath))) {
                                finalFiles.add(files.getString(i));
                                Files.delete(Paths.get(providerFilePath));
                            }
                        } catch (IOException e) {
                            return Completable.error(new InternalErrorThrowable("Could not delete files"));
                        }
                    }

                    termQuery.getJsonObject("term").put("id.keyword", resourceId);
                    filesQuery.getJsonObject("terms").put("data.filename.keyword", finalFiles);
                    filterQuery.add(termQuery);
                    filterQuery.add(filesQuery);
                    baseQuery.getJsonObject("query").getJsonObject("bool").put("filter", filterQuery);

                    return Completable.complete();
                }))
                .andThen(dbService.rxUnpublish(baseQuery))
                .subscribe((result) -> response.setStatusCode(200).end("Ok"), t -> utils.apiFailure(context, t));
        return;
    }
}