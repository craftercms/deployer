/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.impl;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
public class TargetImpl implements Target {

    private static final Logger logger = LoggerFactory.getLogger(TargetImpl.class);

    protected String env;
    protected String siteName;
    protected String id;
    protected DeploymentPipeline deploymentPipeline;
    protected File configurationFile;
    protected Configuration configuration;
    protected ConfigurableApplicationContext applicationContext;
    protected ZonedDateTime loadDate;
    protected ScheduledFuture<?> scheduledFuture;

    public TargetImpl(String env, String siteName, String id, DeploymentPipeline deploymentPipeline, File configurationFile,
                      Configuration configuration, ConfigurableApplicationContext applicationContext) {
        this.env = env;
        this.siteName = siteName;
        this.id = id;
        this.deploymentPipeline = deploymentPipeline;
        this.configurationFile = configurationFile;
        this.configuration = configuration;
        this.applicationContext = applicationContext;
        this.loadDate = ZonedDateTime.now();
    }

    @Override
    public String getEnv() {
        return env;
    }

    @Override
    public String getSiteName() {
        return siteName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public ZonedDateTime getLoadDate() {
        return loadDate;
    }

    @Override
    public File getConfigurationFile() {
        return configurationFile;
    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public synchronized Deployment deploy(Map<String, Object> params) {
        MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, id);
        try {
            return deploymentPipeline.execute(this, params);
        } finally {
            MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
        }
    }

    @Override
    public synchronized void scheduleDeployment(TaskScheduler scheduler, String cronExpression) {
        scheduledFuture = scheduler.schedule(() -> this.deploy(new HashMap<>()), new CronTrigger(cronExpression));
    }

    @Override
    public synchronized void close() {
        MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, id);
        try {
            logger.info("Closing target '{}'...", id);

            if (scheduledFuture != null) {
                scheduledFuture.cancel(true);
            }
            if (applicationContext != null) {
                applicationContext.close();
            }
        } catch (Exception e) {
            logger.error("Failed to close '" + id + "'", e);
        } finally {
            MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
        }
    }

}
