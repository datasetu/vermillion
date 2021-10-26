package vermillion.schedulers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import vermillion.broker.reactivex.BrokerService;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Clock;
import java.util.*;

public class ProviderScheduler implements Job {

    public final Logger logger = LoggerFactory.getLogger(ProviderScheduler.class);
    public final String RABBITMQ_PUBLISH_EXCHANGE = "EXCHANGE";

    public BrokerService brokerService;
    public Vertx vertx;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        vertx = Vertx.vertx();
        brokerService = vermillion.broker.BrokerService.createProxy(vertx.getDelegate(), "broker.queue");

        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        UUID uuid = (UUID) jobDataMap.get("uuid");
        JsonArray finalHits = (JsonArray) jobDataMap.get("finalHits");
        String resourceId = jobDataMap.getString("resourceId");
        List<String> distinctIds = (List<String>) jobDataMap.get("distinctIds");
        Map<String, String> finalQueryParams = (Map<String, String>) jobDataMap.get("finalQueryParams");
        logger.debug("State values: "  + "\n" + uuid + "\n" + resourceId);
        logger.debug("Distinct Ids: "  + distinctIds);
        logger.debug("finalHits to be zipped: " + finalHits);

        if (finalHits.size() > 0) {
            try {
                zipAFileFromItsMetadata(finalHits, uuid, resourceId, distinctIds, finalQueryParams);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String message = "Files are not found, please try with different params";
            throw new JobExecutionException(message);
        }

    }

    private void zipAFileFromItsMetadata(
            JsonArray hits, UUID uuid, String resourceId, List<String> distinctIds, Map<String, String> finalQueryParams) throws IOException {
        zipRequestedFiles(hits, uuid, resourceId, distinctIds, finalQueryParams);
    }

    private void zipRequestedFiles(
            JsonArray hits, UUID uuid, String resourceId, List<String> distinctIds, Map<String, String> finalQueryParams) throws IOException {

        logger.debug("In zipRequestedFiles");

        String PROVIDER_PATH = "/api-server/webroot/provider/";
        String providerPath = PROVIDER_PATH + "public/";

        String providerResourceDir;
        Path providerResourcePath;
        String zippedDirectoryLink;
        List<String> zippedLinks = new ArrayList<>();
        List<String> zippedPaths = new ArrayList<>();

        if (resourceId != null) {
            providerResourceDir = providerPath + resourceId;
            providerResourcePath = Paths.get(providerResourceDir);
            File providerResourceDirectory = new File(providerResourceDir);
            logger.debug("Provider Resource path: " + providerResourceDir);

            if (Files.list(providerResourcePath).findAny().isEmpty()) {
                logger.error("Requested resource ID(s) is not present on provider's resource path");
                throw new FileNotFoundException("Requested files are not present on provider's resource path");
            }

            List<File> filesOnProvider = Arrays.asList(Objects.requireNonNull(providerResourceDirectory.listFiles()));
            logger.debug("files on provider=" + filesOnProvider.toString());
            String zippedDir = providerPath + "/" + uuid + "/"
                    + resourceId.substring(resourceId.lastIndexOf("/"));
            String zippedPath = zippedDir + "/" + resourceId.substring(resourceId.lastIndexOf("/")) + ".zip";
            File zipFileDirectory = new File(zippedDir);
            zipFileDirectory.mkdirs();
            zip(providerResourcePath, null, zippedPath);

            zippedDirectoryLink = "https://" + System.getenv("SERVER_NAME") + "/provider/public/" + uuid;
            logger.debug("Final zipped directory= " + zipFileDirectory.toString());
            logger.debug("Final zipped directory link= " + zippedDirectoryLink);

            publishZipFileAndItsMetadata(resourceId, zippedDirectoryLink, zippedPath, finalQueryParams);
            List<String> lines = Arrays.asList("please wait as your files are getting zipped.",
                    "Once the links are ready, this read me file will be deleted and please " +
                            "refresh this page to find your zips");
            Path write = Files.write(Paths.get(providerPath + "/" + uuid + "/readme.txt"), lines,
                    StandardCharsets.UTF_8);
            logger.debug("Does new file readme file exists before zipping: " + Files.exists(write));

            /*
              Delete the readme.txt file after files are zipped"
             */

//            Vertx vertx = Vertx.vertx();

            long timerId = vertx.setTimer(86400, id -> {
                String PROVIDER_PATH_CONTAINING_README = providerPath + uuid + "/readme.txt";
                logger.debug(" Provider path containing read me: " + PROVIDER_PATH_CONTAINING_README);
                boolean isReadMeDeleted = false;
                boolean isZippedFileDeleted = false;
                try {
                    isReadMeDeleted = Files.deleteIfExists(Paths.get(PROVIDER_PATH_CONTAINING_README));
                    isZippedFileDeleted =  deleteDirectory(new File(providerPath + uuid));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logger.debug("Is read me deleted after zipping files: " + isReadMeDeleted);
                logger.debug("Is zipped file deleted: " + isZippedFileDeleted);
            });


        }
        if (distinctIds != null) {

            for (int i=0; i<distinctIds.size(); i++) {
                providerResourceDir = providerPath + distinctIds.get(i);
                providerResourcePath = Paths.get(providerResourceDir);
                logger.debug("Provider Resource path: " + providerResourcePath.toString());
                if (Files.list(providerResourcePath).findAny().isEmpty()) {
                    logger.error("Requested resource ID(s) is not present on provider's resource path");
                    throw new FileNotFoundException("Requested files are not present on provider's resource path");
                }

                File providerResourceDirectory = new File(providerResourceDir);
                List<File> filesOnProviderPath = Arrays
                        .asList(Objects.requireNonNull(providerResourceDirectory.listFiles()));
                logger.debug("files on provider=" + filesOnProviderPath.toString());
                String zippedDir = providerPath + uuid
                        + distinctIds.get(i).substring(distinctIds.get(i).lastIndexOf("/"));
                String zippedPath = zippedDir + distinctIds.get(i).substring(distinctIds.get(i).lastIndexOf("/")) + ".zip";
                zippedPaths.add(zippedPath);
                File zipFileDirectory = new File(zippedDir);
                zipFileDirectory.mkdirs();
                zip(providerResourcePath,null, zippedPath);

                zippedDirectoryLink = "https://" + System.getenv("SERVER_NAME") + "/provider/public/" + uuid;
                zippedLinks.add(zippedDirectoryLink);
                logger.debug("Final zipped directory= " + zipFileDirectory.toString());

                publishZipFileAndItsMetadata(distinctIds.get(i), zippedDirectoryLink, zippedPath, finalQueryParams);
            }

            logger.debug("zipped links= " + zippedLinks.toString());
            List<String> lines = Arrays.asList("please wait as your files are getting zipped.",
                    "Once the links are ready, this read me file will be deleted and please " +
                            "refresh this page to find your zips");
            Path write = Files.write(Paths.get(providerPath + uuid + "/readme.txt"), lines,
                    StandardCharsets.UTF_8);
            logger.debug("Does new file readme file exists before zipping: " + Files.exists(write));

            /*
              Delete the readme.txt file after files are zipped"
             */

            Vertx vertx = Vertx.vertx();

            long timerId = vertx.setTimer(86400, id -> {
                String PROVIDER_PATH_CONTAINING_README = providerPath + uuid + "/readme.txt";
                logger.debug(" Provider path containing read me: " + PROVIDER_PATH_CONTAINING_README);
                boolean isReadMeDeleted = false;
                boolean isZippedFileDeleted = false;
                try {
                    isReadMeDeleted = Files.deleteIfExists(Paths.get(PROVIDER_PATH_CONTAINING_README));
                    isZippedFileDeleted = deleteDirectory(new File(providerPath + uuid));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logger.debug("Is zipped file deleted: " + isZippedFileDeleted);
                logger.debug("Is read me deleted after zipping files: " + isReadMeDeleted);
            });
        }
    }

    private void publishZipFileAndItsMetadata(
            String resourceId, String zippedDirectoryLink, String zippedPath, Map<String, String> finalQueryParams) {
        String[] splitId = resourceId.split("/");
        String category = splitId[3];
        JsonObject dbEntryJson = new JsonObject()
                .put("data", new JsonObject()
                        .put("link", zippedDirectoryLink)
                        .put("filename", zippedPath.substring(zippedPath.lastIndexOf("/") + 1)))
                .put("timestamp", Clock.systemUTC().instant().toString())
                .put("id", resourceId)
                .put("category", category);
        for(String key : finalQueryParams.keySet()) {
            dbEntryJson.getJsonObject("data").put("metadata", new JsonObject().put(key, finalQueryParams.get(key)));
        }
        logger.debug("zip data published= " + dbEntryJson.encodePrettily());

        brokerService.rxAdminPublish(RABBITMQ_PUBLISH_EXCHANGE, resourceId, dbEntryJson.encode());
    }

    public void zip(Path fileOrFolderToBeZipped, List<File> files, String zipFileName) {
        logger.debug("Start Zip");
        ZipFile zipFile = new ZipFile(zipFileName);
        boolean isDirectory = false;
        if(fileOrFolderToBeZipped!= null) {
            isDirectory = fileOrFolderToBeZipped.toFile().isDirectory();
        }
        try {
            if (isDirectory) {
                zipFile.addFolder(fileOrFolderToBeZipped.toFile());
                logger.debug("Done zipping the folder");
            } else if(files != null) {
                zipFile.addFiles(files);
                logger.debug("Done zipping list of files");
            }
            else {
                zipFile.addFile(fileOrFolderToBeZipped.getFileName().toString());
                logger.debug("Done zipping the file");
            }
        } catch (ZipException e) {
            e.printStackTrace();
            logger.debug("file/folder could not be zipped due to : " + e.getLocalizedMessage());
            return;
        }
        logger.debug("zipped directory: " + zipFile.toString());
        logger.debug("is zip file valid: " + zipFile.isValidZipFile());
        logger.debug("End Zip");
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
}
