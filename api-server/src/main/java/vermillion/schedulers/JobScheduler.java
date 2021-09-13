/*
  Copyright (c) 2021, ARTPARK, Bengaluru
 */
package vermillion.schedulers;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.quartz.*;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

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
        String token = jobDataMap.getString("token");
        UUID uuid = (UUID) jobDataMap.get("uuid");
        String email = jobDataMap.getString("email");
        JsonArray finalHits = (JsonArray) jobDataMap.get("finalHits");
        logger.debug("State values: " + token + "\n" + email  + "\n" + uuid);
        logger.debug("finalHits to be zipped: " + finalHits);

        CONSUMER_PATH+= token;

        if (finalHits.size() > 0) {
            try {
                zipAFileFromItsMetadata(token, finalHits, uuid, email);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String message = "Files are not found, please try with different params";
            throw new JobExecutionException(message);
        }
    }

    private void zipAFileFromItsMetadata(String token, JsonArray hits, UUID uuid, String email) throws IOException {
        zipRequestedFiles(token, hits, uuid, email);
    }

    private void zipRequestedFiles(String token, JsonArray hits, UUID uuid, String email) throws IOException {

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
                    throw new FileNotFoundException("Requested files are not present on provider's resource path");
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
                "Once this links are ready, they will be sent to your email immediately.");
        Path write = Files.write(Paths.get(zipFilePath + "/readme.txt"), lines,
                StandardCharsets.UTF_8);
        logger.debug("Does new file readme file exists before zipping: "  + Files.exists(write));

        logger.debug("list of files to be zipped: " + finalFilesToZip.toString());
        zip(null, finalFilesToZip, zipFilePath+ "/" + file + ".zip");

//        /*
//          Delete the readme.txt file after files are zipped"
//         */
//        String CONSUMER_PATH_CONTAINING_README = zipFilePath + "/readme.txt";
//        logger.debug(" Consumer path containing read me: " + CONSUMER_PATH_CONTAINING_README);
//        boolean b = Files.deleteIfExists(Paths.get(CONSUMER_PATH_CONTAINING_README));
//        logger.debug("Is read me deleted after zipping files: " + b);
        String downloadLink = "https://" + System.getenv("SERVER_NAME") + CONSUMER_PATH;
        emailJob(downloadLink, email);

        logger.debug("final Consumer path: " + CONSUMER_PATH);
    }

    private void zip(Path fileOrFolderToBeZipped, List<File> files, String zipFileName) {
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

    private void emailJob(String downloadLink, String email) {

        logger.debug("In email Job");
        logger.debug("Recipient email= " + email);
        String message = "Your download links are ready. Please use below link to access the files" + "\n" + downloadLink;
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587"); //TLS port
        properties.put("mail.debug", "false"); //enable when you want to see mail logs
        properties.put("mail.smtp.auth", "true"); //enable auth
        properties.put("mail.smtp.starttls.enable", "true"); //enable starttls
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com"); //trust this host
        properties.put("mail.smtp.ssl.protocols", "TLSv1.2"); //specify secure protocol
        final String username = "patzzziejordan@gmail.com";
        final String password = "jordan@4452";
        try{
            Session session = Session.getInstance(properties,
                    new Authenticator(){
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }});

            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(username));
            msg.addRecipient(Message.RecipientType.TO,
                    new InternetAddress("prabhupathrot@gmail.com"));
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
