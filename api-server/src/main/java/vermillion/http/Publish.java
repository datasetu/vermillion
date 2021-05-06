package vermillion.http;

import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.FileUpload;
import io.vertx.reactivex.ext.web.RoutingContext;
import vermillion.util.Utils;
import vermillion.http.HttpServerVerticle;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;

public class Publish extends HttpServerVerticle {

    public final Logger logger = LoggerFactory.getLogger(Publish.class);

    // Auth server constants
    public final String WEBROOT = "webroot/";
    public final String PROVIDER_PATH = "/api-server/webroot/provider/";
    public final String WRITE_SCOPE = "write";

    // RabbitMQ default exchange to publish to
    public final String RABBITMQ_PUBLISH_EXCHANGE = "EXCHANGE";

    public Utils utils = new Utils();

    // Publish API for timeseries data as well as static files
    public void publishFile(RoutingContext context) {
        logger.debug("In publish/file API");
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        FileUpload file = null;
        FileUpload metadata = null;
        JsonObject metaJson = null;

        String fileName = null;
        String resourceId;
        String token;
        JsonObject requestBody = null;
        JsonObject finalRequestBody = new JsonObject();

        HashMap<String, FileUpload> fileUploads = new HashMap<>();

        logger.debug("File uploads = " + context.fileUploads().size());
        logger.debug("Is empty = " + context.fileUploads().isEmpty());
        if (!context.fileUploads().isEmpty()) {
            context.fileUploads().forEach(f -> fileUploads.put(f.name(), f));
            logger.debug(fileUploads.toString());
        }

        if (context.fileUploads().size() > 0) {

            if (context.fileUploads().size() > 2 || !fileUploads.containsKey("file")) {
                utils.apiFailure(context, new BadRequestThrowable("Too many files and/or missing 'file' parameter"));
                // Delete uploaded files if inputs are not as required
                utils.deleteUploads(fileUploads);
                return;
            }
            file = fileUploads.get("file");

            if (fileUploads.containsKey("metadata")) {
                metadata = fileUploads.get("metadata");

                // TODO: Rxify this
                // TODO: File size could crash server. Need to handle this
                Buffer metaBuffer = vert.fileSystem().readFileBlocking(metadata.uploadedFileName());

                try {
                    metaJson = metaBuffer.toJsonObject();
                } catch (Exception e) {
                    utils.apiFailure(context, new BadRequestThrowable("Metadata is not a valid JSON"));
                    utils.deleteUploads(fileUploads);
                    return;
                }
                logger.debug("Metadata = " + metaJson.encode());
            }
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
        if(file == null){
            utils.apiFailure(context, new BadRequestThrowable("No file found"));
            utils.deleteUploads(fileUploads);
            return;
        }
        // TODO: Check for invalid IDs
        resourceId = request.getParam("id");
        token = request.getParam("token");
        if ("".equals(resourceId) || resourceId == null) {
            utils.apiFailure(context, new BadRequestThrowable("No resource ID found in request"));
            utils.deleteUploads(fileUploads);
            return;
        }
        if ("".equals(token) || token == null) {
            utils.apiFailure(context, new BadRequestThrowable("No access token found in request"));
            utils.deleteUploads(fileUploads);
            return;
        }

        if (!utils.isValidToken(token) || !utils.isValidResourceID(resourceId)) {
            utils.apiFailure(context, new UnauthorisedThrowable("Malformed resource ID or token"));
            utils.deleteUploads(fileUploads);
            return;
        }

        String[] splitId = resourceId.split("/");

        // TODO: Need to define the types of IDs supported

        // Rationale: resource ids are structured as domain/sha1/rs.com/category/id
        // Since pre-checks have been done, it is safe to get splitId[3]
        String category = splitId[3];

        JsonArray requestedIdList = new JsonArray().add(resourceId);

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
        checkAuthorisation(token, WRITE_SCOPE, requestedIdList)
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
                .subscribe(() -> response.setStatusCode(201).end("Ok"), t -> utils.apiFailure(context, t));
        return;
    }

    // Publish API for timeseries data as well as static files
    public void publishData(RoutingContext context) {

        logger.debug("In publish/data API");
        HttpServerRequest request = context.request();
        HttpServerResponse response = context.response();

        String resourceId;
        String token;
        JsonObject requestBody = null;
        JsonObject finalRequestBody = new JsonObject();

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

        /* Data should be of the form
             {"data": object, "timestamp": timestamp, "coordinates": [lon, lat],
             "id" (populated by publish API): resource-id,
             "category"(populated by publish API): category}
        */

        Set<String> permittedFieldSet = new HashSet<>();
        permittedFieldSet.add("data");
        permittedFieldSet.add("timestamp");
        permittedFieldSet.add("coordinates");
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

        if (!requestBody.containsKey("data")) {
            utils.apiFailure(context, new BadRequestThrowable("No data field in body"));
            return;
        }

        if (!(requestBody.getValue("id") instanceof String))
        {
            utils.apiFailure(context, new BadRequestThrowable("ID is not valid"));
            return;
        }

        if (!(requestBody.getValue("token") instanceof String))
        {
            utils.apiFailure(context, new BadRequestThrowable("Token is not valid"));
            return;
        }

        if (!(requestBody.getValue("data") instanceof JsonObject)) {
            utils.apiFailure(context, new BadRequestThrowable("Data field is not a JSON object"));
            return;
        }

        if (!permittedFieldSet.containsAll(requestBody.fieldNames())) {
            utils.apiFailure(context, new BadRequestThrowable("Body contains unnecessary fields"));
            return;
        }

        // TODO: Check for invalid IDs
        resourceId = requestBody.getString("id");
        token = requestBody.getString("token");
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

        String[] splitId = resourceId.split("/");

        // TODO: Need to define the types of IDs supported

        // Rationale: resource ids are structured as domain/sha1/rs.com/category/id
        // Since pre-checks have been done, it is safe to get splitId[3]
        String category = splitId[3];

        if(requestBody.containsKey("coordinates")){
            finalRequestBody.put("coordinates",requestBody.getValue("coordinates"));
        }

        finalRequestBody
                .put("data",requestBody.getValue("data"))
                .put("timestamp", requestBody.getValue("timestamp", Clock.systemUTC().instant().toString()))
                .put("id", resourceId)
                .put("category", category)
                .put("mime-type", "application/json");

        // There is no need for introspect here. It will be done at the rmq auth backend level
        brokerService
                .rxPublish(token, RABBITMQ_PUBLISH_EXCHANGE, resourceId, finalRequestBody.encode())
                .subscribe(() -> response.setStatusCode(202).end(), t -> utils.apiFailure(context, t));
    }
}