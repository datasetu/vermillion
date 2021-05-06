package vermillion.http;

import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.web.RoutingContext;
import vermillion.util.Utils;
import vermillion.http.HttpServerVerticle;
import vermillion.throwables.BadRequestThrowable;
import vermillion.throwables.InternalErrorThrowable;
import vermillion.throwables.UnauthorisedThrowable;

import java.io.File;
import java.nio.file.*;
import java.util.*;

public class Download extends HttpServerVerticle {

    public final Logger logger = LoggerFactory.getLogger(Download.class);

    public final String WEBROOT = "webroot/";
    public final String PROVIDER_PATH = "/api-server/webroot/provider/";
    public final String READ_SCOPE = "read";

    public String CONSUMER_PATH = "/consumer/";

    public Utils utils = new Utils();

    // TODO: If Id is provided, reroute to the specific file
    public void download(RoutingContext context) {

        HttpServerRequest request = context.request();

        // If token is valid for resources apart from secure 'files' then specify list of ids in the
        // request
        String token = request.getParam("token");
        String idParam = request.getParam("id");

        logger.debug("token=" + token);

        if (token == null) {
            utils.apiFailure(context, new BadRequestThrowable("No access token found in request"));
            return;
        }

        if (!utils.isValidToken(token)) {
            utils.apiFailure(context, new UnauthorisedThrowable("Malformed access token"));
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

            if (!utils.isValidResourceID(resourceIDStr)) {
                utils.apiFailure(context, new BadRequestThrowable("Malformed resource ID"));
                return;
            }

            if (resourceIDStr.endsWith(".public")) {
                utils.apiFailure(
                        context,
                        new BadRequestThrowable(
                                "This API is for secure resources only. Use /provider/public endpoint to explore public data"));
                return;
            }
        }

        logger.debug("Requested IDs Json =" + requestedIds.encode());

        CONSUMER_PATH = "/consumer/" + token + "/";

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

                        for (int i = 0; i < authorisedIds.size(); i++) {

                            String resourceId = authorisedIds.getString(i);
                            // Get the actual file name on disk

                            String consumerResourceDir = WEBROOT + "consumer/" + token + "/" + resourceId;

                            // Create consumer directory path if it does not exist
                            new File(consumerResourceDir).mkdirs();
                            logger.debug("Created consumer subdirectory");

                            Path consumerResourcePath = Paths.get(WEBROOT + "consumer/" + token + "/" + resourceId);
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
                        if (!authorisedIds.isEmpty()) {

                            // If only one id is there return the file.
                            if (authorisedIds.size() == 1) {
                                CONSUMER_PATH += authorisedIds.getString(0) + "/";
                            } else {

                                // Getting common directory for all ids
                                String authorisedIdPrefix = utils.commonPrefix(authorisedIds);
                                int lastDirIndex = authorisedIdPrefix.lastIndexOf('/');
                                if (lastDirIndex != -1) {
                                    CONSUMER_PATH += authorisedIdPrefix.substring(0, lastDirIndex + 1);
                                }
                            }
                        }

                        return Completable.complete();
                    })
                    .subscribe(() -> context.reroute(CONSUMER_PATH), t -> utils.apiFailure(context, t));
        } else {
            checkAuthorisation(token, READ_SCOPE, requestedIds)
                    .andThen(Completable.fromCallable(() -> {
                        logger.debug("Requested IDs = " + requestedIds.encode());
                        for (int i = 0; i < requestedIds.size(); i++) {
                            logger.debug("File=" + basePath + requestedIds.getString(i));
                            if (Files.notExists(Paths.get(basePath + requestedIds.getString(i)))) {
                                return Completable.error(
                                        new UnauthorisedThrowable("Requested resource ID(s) is not present"));
                            }
                        }

                        for (int i = 0; i < requestedIds.size(); i++) {
                            String resourceId = requestedIds.getString(i);
                            String consumerResourceDir = WEBROOT + "consumer/" + token + "/"
                                    + resourceId.substring(0, resourceId.lastIndexOf('/'));

                            // Create consumer directory path if it does not exist
                            new File(consumerResourceDir).mkdirs();
                            logger.debug("Insubdirectory");

                            Path consumerResourcePath = Paths.get(WEBROOT + "consumer/" + token + "/" + resourceId);
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
                                String requestedIdPrefix = utils.commonPrefix(requestedIds);
                                int lastDirIndex = requestedIdPrefix.lastIndexOf('/');
                                if (lastDirIndex != -1) {
                                    CONSUMER_PATH += requestedIdPrefix.substring(0, lastDirIndex + 1);
                                }
                            }
                        }

                        return Completable.complete();
                    }))
                    .subscribe(() -> context.reroute(CONSUMER_PATH), t -> utils.apiFailure(context, t));
        }
    }
}