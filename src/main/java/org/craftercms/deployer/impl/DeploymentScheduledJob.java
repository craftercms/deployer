package org.craftercms.deployer.impl;

import org.craftercms.deployer.api.DeploymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Created by alfonsovasquez on 1/3/17.
 */
@Component("deploymentScheduledJob")
public class DeploymentScheduledJob {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentScheduledJob.class);

    @Autowired
    protected DeploymentService deploymentService;

    @Scheduled(cron = "${deployer.main.scheduledJob.cron}")
    public void run() {
        logger.debug("Executing deployment cron job...");

        try {
            deploymentService.deployAllSites();
        } catch (Exception e) {
            logger.error("Error while executing deployment cron job", e);
        }
    }

}
