/*
  Copyright (c) 2021, ARTPARK, Bengaluru
 */
package vermillion.schedulers;

import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static vermillion.database.DbService.createProxy;

/**
 * @Author Prabhu A Patrot
 */
public class JobScheduler implements Job {

    public final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    public vermillion.database.reactivex.DbService dbService;

    String CONSUMER_PATH = "/consumer/";

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        logger.debug("Inside scheduler");

        String threadName = Thread.currentThread().getName();
        long threadId = Thread.currentThread().getId();
        logger.debug("Thread info: " + threadName + "\n" + threadId);

        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();
        logger.debug("map: " + jobDataMap.getString("token"));
        String token = jobDataMap.getString("token");
        JsonObject downloadByQuery = (JsonObject) jobDataMap.get("query");
        String scrollDuration = jobDataMap.getString("scrollDuration");
        Object vertx = jobDataMap.get("vertxContext");
        RoutingContext context = (RoutingContext) jobDataMap.get("routingContext");
        boolean scroll = jobDataMap.getBoolean("scroll");
        UUID uuid = (UUID) jobDataMap.get("uuid");
        logger.debug("State values: " + token + "\n" + downloadByQuery + "\n" + scrollDuration + "\n" + context + "\n" + uuid);

        if (vertx instanceof Vertx) {
            dbService = createProxy(((Vertx) vertx).getDelegate(), "db.queue");
        }

        CONSUMER_PATH+= token;

        dbService.rxSecureSearch(downloadByQuery, token, scroll, scrollDuration)
                .flatMapCompletable(result -> {
                    logger.debug("Response from search endpoint:" + result.toString());

                    JsonArray hits = result.getJsonArray("hits");

                    if (hits.size() > 0) {
                        zipAFileFromItsMetadata(context, token, hits, uuid);
                    } else {
                        return Completable.error(new FileNotFoundException("Requested resource ID(s)/File(s) is not present"));
                    }
                    return Completable.complete();
                }).subscribe(() -> {
                    String downloadLink = "https://" + System.getenv("SERVER_NAME") + CONSUMER_PATH;
                    logger.debug("download link: " + downloadLink);
                },
                throwable -> {
                    JobExecutionException jobExecutionException = new JobExecutionException(throwable);
                    logger.debug("error caused due to: " + jobExecutionException.getLocalizedMessage());
                    throw jobExecutionException;
                });

    }

    private void zipAFileFromItsMetadata(RoutingContext context, String token, JsonArray hits, UUID uuid) throws IOException {

        zipRequestedFiles(context, token, hits, uuid);

    }

    private void zipRequestedFiles(RoutingContext context, String token, JsonArray hits, UUID uuid) throws IOException {

        logger.debug("In zipRequestedFiles");

        String PROVIDER_PATH = "/api-server/webroot/provider/";
        String providerPath = PROVIDER_PATH + "secure/";
        String webroot = "webroot/";

        String consumerResourceDir;
        String providerResourceDir;
        Path consumerResourcePath;
        Path providerResourcePath;
        String finalPathFromMetadata;
        String file = "";
        List<File> finalFilesToZip = new ArrayList<>();
        for (Object hit1: hits) {
            if(hit1 instanceof JsonObject) {
                logger.debug("hit: " + hit1.toString());
                file = ((JsonObject) hit1).getJsonObject("data").getString("filename");
                logger.debug("file=" + file);
                finalPathFromMetadata = ((JsonObject) hit1).getString("id");
                logger.debug("Final path to file from metadata: " + finalPathFromMetadata + "/" + file);
                consumerResourceDir = webroot + "consumer/" + token + "/"
                        + finalPathFromMetadata + "/" + uuid  ;
                providerResourceDir = providerPath + finalPathFromMetadata + "/" + file;

                File consumerResourceDirectory = new File(consumerResourceDir);
                consumerResourceDirectory.mkdirs();

                consumerResourcePath = Paths.get(consumerResourceDir);
                providerResourcePath = Paths.get(providerResourceDir);

                logger.debug("Consumer Resource path: " + consumerResourcePath.toString());
                logger.debug("Provider Resource path: " + providerResourcePath.toString());

                if (Files.notExists(Paths.get(String.valueOf(providerResourcePath)))) {
                    logger.error("Requested resource ID(s) is not present on provider's resource path");
                    throw new FileNotFoundException("Requested resource ID(s) is not present");
                } else {
                    finalFilesToZip.add(providerResourcePath.toFile());
                }
            }
        }

        String zipFilePath = webroot + "consumer/" + token + "/" + uuid ;
        File zipFileDirectory = new File(zipFilePath);
        zipFileDirectory.mkdirs();

        CONSUMER_PATH = CONSUMER_PATH + "/" + uuid + "/" + file +".zip";
        List<String> lines = Arrays.asList("please wait as your files are getting zipped.",
                "Once this file disappears(when you see resource not found), the files will be zipped and kept in the same directory link");
        Path write = Files.write(Paths.get(zipFilePath + "/readme.txt"), lines,
                StandardCharsets.UTF_8);
        logger.debug("Does new file readme file exists before zipping: "  + Files.exists(write));

        logger.debug("list of files to be zipped: " + finalFilesToZip.toString());
        zip(context, null, finalFilesToZip, zipFilePath+ "/" + file + ".zip");

        /*
          Delete the readme.txt file after files are zipped"
         */
        String CONSUMER_PATH_CONTAINING_README = zipFilePath + "/readme.txt";
        logger.debug(" Consumer path containing read me: " + CONSUMER_PATH_CONTAINING_README);
        boolean b = Files.deleteIfExists(Paths.get(CONSUMER_PATH_CONTAINING_README));
        logger.debug("Is read me deleted after zipping files: " + b);

        logger.debug("final Consumer path: " + CONSUMER_PATH);
    }

    private void zip(RoutingContext context, Path fileOrFolderToBeZipped, List<File> files, String zipFileName) {
        logger.debug("Start Zip");
        ZipFile zipFile = new ZipFile(zipFileName);
        boolean isDirectory = false;
        if(fileOrFolderToBeZipped!= null) {
            isDirectory = fileOrFolderToBeZipped.toFile().isDirectory();
        }
        logger.debug("Is fileOrFolderToBeZipped directory: " + isDirectory);
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
        logger.debug("is valid zip file: " + zipFile.isValidZipFile());
        logger.debug("End Zip");
    }
}
