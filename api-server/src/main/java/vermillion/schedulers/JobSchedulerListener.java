/*
  Copyright (c) 2021, ARTPARK, Bengaluru
 */
package vermillion.schedulers;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.quartz.*;

/**
 * @Author Prabhu A Patrot
 */
public class JobSchedulerListener implements JobListener {

    public final Logger logger = LoggerFactory.getLogger(JobSchedulerListener.class);
    public static final String LISTENER_NAME = "downloadByQuery";

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext jobExecutionContext) {
        JobDetail jobDetail = jobExecutionContext.getJobDetail();
        logger.debug("Job detail: " + jobDetail.toString());

    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext jobExecutionContext) {

    }

    @Override
    public void jobWasExecuted(JobExecutionContext jobExecutionContext, JobExecutionException e) {
        Object result = jobExecutionContext.getResult();
        logger.debug("result: " + result.toString());
    }
}
