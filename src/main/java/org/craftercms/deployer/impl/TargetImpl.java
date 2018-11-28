/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.utils.GitUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

/**
 * Default implementation of {@link Target}.
 *
 * @author avasquez
 */
public class TargetImpl implements Target {

    private static final Logger logger = LoggerFactory.getLogger(TargetImpl.class);

    public static final String TARGET_ID_FORMAT = "%s-%s";

    protected String env;
    protected String siteName;
    protected String localRepoPath;
    protected DeploymentPipeline deploymentPipeline;
    protected File configurationFile;
    protected Configuration configuration;
    protected ConfigurableApplicationContext applicationContext;
    protected ZonedDateTime loadDate;
    protected ScheduledFuture<?> scheduledDeploymentFuture;
    protected ExecutorService deploymentExecutor;
    protected Queue<Deployment> pendingDeployments;
    protected volatile Deployment currentDeployment;

    protected final Lock deploymentLock = new ReentrantLock();

    public static String getId(String env, String siteName) {
        return String.format(TARGET_ID_FORMAT, siteName, env);
    }

    public TargetImpl(String env, String siteName, String localRepoPath, DeploymentPipeline deploymentPipeline,
                      File configurationFile, Configuration configuration,
                      ConfigurableApplicationContext applicationContext, ExecutorService deploymentExecutor) {
        this.env = env;
        this.siteName = siteName;
        this.localRepoPath = localRepoPath;
        this.deploymentPipeline = deploymentPipeline;
        this.configurationFile = configurationFile;
        this.configuration = configuration;
        this.applicationContext = applicationContext;
        this.loadDate = ZonedDateTime.now();
        this.deploymentExecutor = deploymentExecutor;
        this.pendingDeployments = new ConcurrentLinkedQueue<>();
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
        return getId(env, siteName);
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
    public Deployment deploy(boolean waitTillDone, Map<String, Object> params) {
        Deployment deployment = new Deployment(this, params);
        pendingDeployments.add(deployment);

        Future<?> future = deploymentExecutor.submit(new DeploymentTask());
        if (waitTillDone) {
            logger.debug("Waiting for deployment completion...");

            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Unable to wait for deployment completion", e);
            }
        }

        return deployment;
    }

    @Override
    public void scheduleDeployment(TaskScheduler scheduler, String cronExpression) {
        scheduledDeploymentFuture = scheduler.schedule(new ScheduledDeploymentTask(), new CronTrigger(cronExpression));
    }

    @Override
    public Collection<Deployment> getPendingDeployments() {
        return new ArrayList<>(pendingDeployments);
    }

    @Override
    public Deployment getCurrentDeployment() {
        return currentDeployment;
    }

    @Override
    public Collection<Deployment> getAllDeployments() {
        Collection<Deployment> deployments = new ArrayList<>();
        Deployment currentDeployment = getCurrentDeployment();
        Collection<Deployment> pendingDeployments = getPendingDeployments();

        if (currentDeployment != null) {
            deployments.add(currentDeployment);
        }
        if (CollectionUtils.isNotEmpty(pendingDeployments)) {
            deployments.addAll(pendingDeployments);
        }

        return deployments;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup() {
        MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, getId());
        try {
            logger.info("Cleaning up repo for target {}", getId());
            GitUtils.cleanup(localRepoPath);
        } catch (Exception e) {
            logger.warn("Error cleaning up repo for target {}", getId());
        }
        MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
    }

    @Override
    public void close() {
        MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, getId());

        try {
            logger.info("Closing target '{}'...", getId());

            if (scheduledDeploymentFuture != null) {
                scheduledDeploymentFuture.cancel(true);
            }

            deploymentPipeline.destroy();

            if (applicationContext != null) {
                applicationContext.close();
            }
        } catch (Exception e) {
            logger.error("Failed to close '" + getId() + "'", e);
        }

        MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
    }

    protected class ScheduledDeploymentTask implements Runnable {

        protected volatile Future<?> future;

        @Override
        public void run() {
            if (future == null || future.isDone() && currentDeployment == null) {
                pendingDeployments.add(new Deployment(TargetImpl.this));

                future = deploymentExecutor.submit(new DeploymentTask());
            } else {
                logger.info("Active deployment detected, skipping scheduled deployment for target {}", getId());
            }
        }

    }

    protected class DeploymentTask implements Runnable {

        @Override
        public void run() {
            deploymentLock.lock();
            currentDeployment = pendingDeployments.remove();

            MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, getId());

            try {

                logger.info("============================================================");
                logger.info("Deployment for {} started", getId());
                logger.info("============================================================");

                deploymentPipeline.execute(currentDeployment);

                double durationInSecs = currentDeployment.getDuration() / 1000.0;

                logger.info("============================================================");
                logger.info("Deployment for {} finished in {} secs", getId(), String.format("%.3f", durationInSecs));
                logger.info("============================================================");
            } finally {
                currentDeployment = null;

                MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
                
                deploymentLock.unlock();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TargetImpl target = (TargetImpl)o;

        if (!env.equals(target.env)) {
            return false;
        }
        if (!siteName.equals(target.siteName)) {
            return false;
        }
        if (!configurationFile.equals(target.configurationFile)) {
            return false;
        }

        return loadDate.equals(target.loadDate);
    }

    @Override
    public int hashCode() {
        int result = env.hashCode();
        result = 31 * result + siteName.hashCode();
        result = 31 * result + configurationFile.hashCode();
        result = 31 * result + loadDate.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TargetImpl{" +
               "env='" + env + '\'' +
               ", siteName='" + siteName + '\'' +
               ", configurationFile=" +
               configurationFile + '}';
    }

}
