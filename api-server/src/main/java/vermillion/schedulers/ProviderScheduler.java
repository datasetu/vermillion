package vermillion.schedulers;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.redis.client.Redis;
import io.vertx.reactivex.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ProviderScheduler implements Job {

    public final Logger logger = LoggerFactory.getLogger(ProviderScheduler.class);

    public Vertx vertx;

    public RedisOptions options;
    public final String REDIS_PASSWORD = System.getenv("REDIS_PASSWORD");
    public final String REDIS_HOST = System.getenv("REDIS_HOSTNAME");
    // There are 16 DBs available. Using 1 as the default database number
    public final String DB_NUMBER = "1";
    public final int MAX_POOL_SIZE = 10;
    public final int MAX_WAITING_HANDLERS = 32;
    public final String CONNECTION_STR = "redis://:" + REDIS_PASSWORD + "@" + REDIS_HOST + "/" + DB_NUMBER;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        logger.debug("In Provider Scheduler");

        vertx = Vertx.vertx();
        options = new RedisOptions()
                .setConnectionString(CONNECTION_STR)
                .setMaxPoolSize(MAX_POOL_SIZE)
                .setMaxWaitingHandlers(MAX_WAITING_HANDLERS);

        JobDataMap jobDataMap = jobExecutionContext.getJobDetail().getJobDataMap();

        UUID uuid = (UUID) jobDataMap.get("uuid");
        int finalHitsSize = jobDataMap.getInt("finalHitsSize");
        String resourceId = jobDataMap.getString("resourceId");
        String email = jobDataMap.getString("email");
        List<String> distinctIds = (List<String>) jobDataMap.get("distinctIds");
        List<File> listOfFilesNeedToBeZipped = (List<File>) jobDataMap.get("listOfFilesNeedToBeZipped");
        List<String> finalZipLinks = (List<String>) jobDataMap.get("finalZipLinks");

        logger.debug("State values: "  + "\n" + uuid + "\n" + resourceId + "\n" + email);
        logger.debug("Distinct Ids: "  + distinctIds);
        logger.debug("final zip links: "  + finalZipLinks);
        logger.debug("finalHitsSize to be zipped: " + finalHitsSize);

        if (finalHitsSize > 0) {
            try {
                zipAFileFromItsMetadata(uuid, resourceId, distinctIds, listOfFilesNeedToBeZipped, email, finalZipLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String message = "Files are not found, please try with different params";
            throw new JobExecutionException(message);
        }

    }

    private void zipAFileFromItsMetadata(UUID uuid, String resourceId, List<String> distinctIds,
                                         List<File> listOfFilesNeedToBeZipped, String email,
                                         List<String> finalZipLinks) throws IOException {
        logger.debug("In zipAFileFromItsMetadata");
        zipRequestedFiles(uuid, resourceId, distinctIds, listOfFilesNeedToBeZipped, email, finalZipLinks);
    }

    private void zipRequestedFiles(UUID uuid, String resourceId, List<String> distinctIds, List<File> listOfFilesNeedToBeZipped, String email, List<String> finalZipLinks) throws IOException {

        logger.debug("In zipRequestedFiles");

        String PROVIDER_PATH = "/api-server/webroot/provider/";
        String providerPath = PROVIDER_PATH + "public/";

        String providerResourceDir;
        Path providerResourcePath;
        String zippedDirectoryLink;
        List<String> zippedLinks = new ArrayList<>();
        List<String> zippedPaths = new ArrayList<>();
        Map<String, List<File>> hashMap = new HashMap<>();
        List<File> finalListOfFilesNeedToBeZipped;

        if (resourceId != null) {
            providerResourceDir = providerPath + resourceId;
            providerResourcePath = Paths.get(providerResourceDir);
            File providerResourceDirectory = new File(providerResourceDir);
            logger.debug("Provider Resource path: " + providerResourceDir);

            if (Files.list(providerResourcePath).findAny().isEmpty()) {
                logger.error("Requested resource ID(s) is not present on provider's resource path");
                throw new FileNotFoundException("Requested files are not present on provider's resource path");
            }

//            List<File> filesOnProvider = Arrays.asList(Objects.requireNonNull(providerResourceDirectory.listFiles()));
//            logger.debug("files on provider=" + filesOnProvider.toString());
            String zippedDir = providerPath + uuid
                    + resourceId.substring(resourceId.lastIndexOf("/"));
            String zippedPath = zippedDir + resourceId.substring(resourceId.lastIndexOf("/")) + ".zip";
            logger.debug("zippedDir=" + zippedDir);
            logger.debug("zippedPath=" + zippedPath);
            File zipFileDirectory = new File(zippedDir);
            zipFileDirectory.mkdirs();

            zip(providerResourcePath, null, zippedPath);

            zippedDirectoryLink = "https://" + System.getenv("SERVER_NAME") + zippedPath.substring(19);
            logger.debug("Final zipped directory= " + zipFileDirectory.toString());
            logger.debug("Final zipped directory link= " + zippedDirectoryLink);

            setValue(resourceId, zippedPath).subscribe();

            emailJob(zippedDirectoryLink, null, email);

//            Vertx vertx = Vertx.vertx();

            long timerId = vertx.setTimer(86400000, id -> {
                boolean isZippedFileDeleted;
                isZippedFileDeleted =  deleteDirectory(new File(providerPath + uuid));
                logger.debug("Is zipped file deleted: " + isZippedFileDeleted);
            });
        }

        if (!listOfFilesNeedToBeZipped.isEmpty()) {

            int counter = 0;
            for(int i=0; i<distinctIds.size(); i++) {
                finalListOfFilesNeedToBeZipped = new ArrayList<>();
                for(int j=0; j<listOfFilesNeedToBeZipped.size(); j++) {

                    if (i < 1) {
                        providerResourceDir = listOfFilesNeedToBeZipped.get(j).getAbsolutePath();
                        providerResourcePath = Paths.get(providerResourceDir);
                        logger.debug("Provider Resource path: " + providerResourcePath.toString());

                        if (Files.notExists(Paths.get(String.valueOf(providerResourcePath)))) {
                            counter++;
                            logger.error("Requested resource ID(s) is not present on provider's resource path" + providerResourcePath);
                        }
                    }

                    if (counter == listOfFilesNeedToBeZipped.size()) {
                        throw new FileNotFoundException("Requested files are not present on provider's resource path");
                    }

                    if(listOfFilesNeedToBeZipped.get(j).toString().contains(distinctIds.get(i))) {
                        finalListOfFilesNeedToBeZipped.add(listOfFilesNeedToBeZipped.get(j));
                        hashMap.put(distinctIds.get(i), finalListOfFilesNeedToBeZipped);
                    }
                }
            }

            for (String id : hashMap.keySet()) {

                logger.debug("segregated id =" + id);
                String zippedDir = providerPath + uuid + id.substring(id.lastIndexOf("/"));
                String zippedPath = zippedDir + id.substring(id.lastIndexOf("/")) + ".zip";
                zippedPaths.add(zippedPath);
                File zipFileDirectory = new File(zippedDir);
                zipFileDirectory.mkdirs();

                zip(null, hashMap.get(id), zippedPath);

                zippedDirectoryLink = "https://" + System.getenv("SERVER_NAME") + zippedPath.substring(19);
                zippedLinks.add(zippedDirectoryLink);
                logger.debug("Final zipped directory= " + zipFileDirectory.toString());

                setValue(id, zippedPath).subscribe();
            }
            zippedLinks.addAll(finalZipLinks);
            logger.debug("zipped links= " + zippedLinks.toString());
            logger.debug("zipped paths= " + zippedPaths.toString());

            emailJob(null, zippedLinks, email);

//            Vertx vertx = Vertx.vertx();
            long timerId = vertx.setTimer(86400000, id -> {
                boolean isZippedFileDeleted;
                File directoryToBeDeleted = new File(providerPath + uuid);
                isZippedFileDeleted = deleteDirectory(directoryToBeDeleted);
                logger.debug("file directory to be deleted = " + directoryToBeDeleted.getName());
                logger.debug("Is zipped file directory deleted: " + isZippedFileDeleted);
            });
        }
    }


    public Single<RedisAPI> getRedisClient() {
        logger.debug("In get redis client");
        logger.debug("options=" + options.toJson().encodePrettily());
        return Redis.createClient(vertx, options).rxConnect().map(RedisAPI::api);
    }
    public Completable setValue(String key, String value) {

        logger.debug("In set value");
        logger.debug("key=" + key);
        logger.debug("value=" + value);
        ArrayList<String> list = new ArrayList<>();

        list.add(key);
        list.add(value);

        return getRedisClient().flatMapCompletable(redisAPI -> Completable.fromMaybe(redisAPI.rxSet(list)));
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
    private void emailJob(String downloadLink, List<String> downloadLinks, String email) {

        logger.debug("In email Job");
        logger.debug("Recipient email= " + email);
        String message = "";
        if (downloadLink!=null) {
            message = "Dear consumer,"
                    + "\n" + "The downloadable links for the datasets you requested are ready to be served. Please use below link to download the datasets as a zip file."
                    + "\n" + downloadLink;
        }
        if (downloadLinks!=null) {
            message = "Dear consumer,"
                    + "\n" + "The downloadable links for the datasets you requested are ready to be served. Please use below link to download the datasets as a zip file."
                    + "\n" + downloadLinks;
        }
        Properties properties = new Properties();
        properties.put("mail.smtp.host", System.getenv("HOST")); //host name
        properties.put("mail.smtp.port", System.getenv("EMAIL_PORT")); //TLS port
        properties.put("mail.debug", "false"); //enable when you want to see mail logs
        properties.put("mail.smtp.auth", "true"); //enable auth
        properties.put("mail.smtp.starttls.enable", "true"); //enable starttls
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com"); //trust this host
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
            msg.setText(message);
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
}
