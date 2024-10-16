/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.git.utils.GitUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetNotReadyException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.craftercms.commons.config.ConfigUtils.getBooleanProperty;
import static org.craftercms.commons.config.ConfigUtils.getStringProperty;
import static org.craftercms.deployer.impl.DeploymentConstants.*;

/**
 * Default implementation of {@link Target}.
 *
 * @author avasquez
 */
public class TargetImpl implements Target {

    private static final Logger logger = LoggerFactory.getLogger(TargetImpl.class);

    private static final ThreadLocal<Target> threadLocal = new InheritableThreadLocal<>();

    public static final String TARGET_ID_FORMAT = "%s-%s";

    protected final ZonedDateTime loadDate;
    protected final String env;
    protected final String siteName;
    protected final String localRepoPath;
    protected final File configurationFile;
    protected final HierarchicalConfiguration<ImmutableNode> configuration;
    protected final ConfigurableApplicationContext applicationContext;
    protected final ExecutorService executor;
    protected final TaskScheduler scheduler;
    protected final TargetLifecycleHooksResolver targetLifecycleHooksResolver;
    protected final DeploymentPipelineFactory deploymentPipelineFactory;

    protected volatile Status status;
    protected DeploymentPipeline deploymentPipeline;
    protected ScheduledFuture<?> scheduledDeploymentFuture;
    protected final Queue<Deployment> pendingDeployments;
    protected volatile Deployment currentDeployment;
    protected final Lock deploymentLock;

    public static void setCurrent(Target target) {
        threadLocal.set(target);
    }

    public static Target getCurrent() {
        return threadLocal.get();
    }

    public static void clear() {
        threadLocal.set(null);
    }

    public static String getId(String env, String siteName) {
        return String.format(TARGET_ID_FORMAT, siteName, env);
    }

    public TargetImpl(
            @Value("${target.env}") String env,
            @Value("${target.siteName}") String siteName,
            @Value("${target.localRepoPath}") String localRepoPath,
            @Value("${target.configFile}") File configurationFile,
            @Autowired HierarchicalConfiguration<ImmutableNode> configuration,
            @Autowired ConfigurableApplicationContext applicationContext,
            @Autowired ExecutorService executor,
            @Autowired TaskScheduler scheduler,
            @Autowired TargetLifecycleHooksResolver targetLifecycleHooksResolver,
            @Autowired DeploymentPipelineFactory deploymentPipelineFactory) {
        this.loadDate = ZonedDateTime.now();
        this.env = env;
        this.siteName = siteName;
        this.localRepoPath = localRepoPath;
        this.configurationFile = configurationFile;
        this.configuration = configuration;
        this.applicationContext = applicationContext;
        this.executor = executor;
        this.scheduler = scheduler;
        this.targetLifecycleHooksResolver = targetLifecycleHooksResolver;
        this.deploymentPipelineFactory = deploymentPipelineFactory;
        this.status = Status.CREATED;
        this.pendingDeployments = new ConcurrentLinkedQueue<>();
        this.deploymentLock = new ReentrantLock();
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
    public Status getStatus() {
        return status;
    }

    @Override
    public File getConfigurationFile() {
        return configurationFile;
    }

    @Override
    public HierarchicalConfiguration<ImmutableNode> getConfiguration() {
        return configuration;
    }

    @Override
    public ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void init() {
        MDC.put(TARGET_ID_MDC_KEY, getId());

        status = Status.INIT_IN_PROGRESS;

        try {
            logger.info("Executing init hooks for target '{}'", getId());

            executeHooks(getInitHooks());

            logger.info("Creating deployment pipeline for target '{}'", getId());

            deploymentPipeline = deploymentPipelineFactory.getPipeline(configuration, applicationContext,
                                                                       TARGET_DEPLOYMENT_PIPELINE_CONFIG_KEY);

            logger.info("Checking if deployments need to be scheduled for target '{}'", getId());

            scheduleDeployments();

            status = Status.INIT_COMPLETED;
        } catch (Exception e) {
            status = Status.INIT_FAILED;

            logger.error("Failed to init target '" + getId() + "'", e);
        }

        MDC.remove(TARGET_ID_MDC_KEY);
    }

    void executeCreateHooks() throws ConfigurationException, DeployerException {
        logger.info("Executing create hooks for target '{}'", getId());
        executeHooks(getCreateHooks());
        logger.info("Create hooks executed for target '{}'", getId());
    }

    void executeDuplicateHooks() throws Exception {
        logger.info("Executing duplicate hooks for target '{}'", getId());
        executeHooks(getDuplicateHooks());
        logger.info("Duplicate hooks executed for target '{}'", getId());
    }

    private void executeHooks(final Collection<TargetLifecycleHook> hooks) throws DeployerException {
        for (TargetLifecycleHook hook : hooks) {
            hook.execute(this);
        }
    }

    @Override
    public Deployment deploy(boolean waitTillDone, Map<String, Object> params) throws TargetNotReadyException {
        if (status == Status.INIT_COMPLETED) {
            Deployment deployment = new Deployment(this, params);
            pendingDeployments.add(deployment);

            Future<?> future = executor.submit(new DeploymentTask());
            if (waitTillDone) {
                logger.debug("Waiting for deployment completion...");

                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Unable to wait for deployment completion", e);
                }
            }

            return deployment;
        } else {
            throw new TargetNotReadyException("The target is not ready yet for deployments (status " + status + ")");
        }
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
    public void cleanRepo() {
        MDC.put(TARGET_ID_MDC_KEY, getId());

        try {
            logger.info("Cleaning up repo for target {}", getId());
            GitUtils.cleanup(localRepoPath);
        } catch (Exception e) {
            logger.warn("Error cleaning up repo for target {}", getId());
        }

        MDC.remove(TARGET_ID_MDC_KEY);
    }

    @Override
    public void close() {
        MDC.put(TARGET_ID_MDC_KEY, getId());

        try {
            logger.info("Closing target '{}'...", getId());
            logger.info("Stopping current and pending deployments for target '{}'", getId());

            stopDeployments();

            logger.info("Releasing resources for target '{}'", getId());

            if (scheduledDeploymentFuture != null) {
                scheduledDeploymentFuture.cancel(true);
            }

            if (deploymentPipeline != null) {
                deploymentPipeline.destroy();
            }

            if (applicationContext != null) {
                applicationContext.close();
            }
        } catch (Exception e) {
            logger.error("Failed to close '" + getId() + "'", e);
        }

        MDC.remove(TARGET_ID_MDC_KEY);
    }

    @Override
    public void delete() {
        MDC.put(TARGET_ID_MDC_KEY, getId());

        status = Status.DELETE_IN_PROGRESS;

        try {
            logger.info("Deleting target '{}'...", getId());
            logger.info("Stopping current and pending deployments for target '{}'", getId());

            stopDeployments();

            logger.info("Executing delete hooks for target '{}'", getId());

            executeHooks(getDeleteHooks());

            logger.info("Releasing resources for target '{}'", getId());

            if (scheduledDeploymentFuture != null) {
                scheduledDeploymentFuture.cancel(true);
            }

            if (deploymentPipeline != null) {
                deploymentPipeline.destroy();
            }

            if (applicationContext != null) {
                applicationContext.close();
            }
        } catch (Exception e) {
            logger.error("Failed deleting target '" + getId() + "'", e);
        } finally {
            status = Status.DELETED;
        }

        MDC.remove(TARGET_ID_MDC_KEY);
    }

    @Override
    public void unlock() {
        MDC.put(TARGET_ID_MDC_KEY, getId());

        try {
            if (GitUtils.isRepositoryLocked(localRepoPath)) {
                GitUtils.unlock(localRepoPath);
            }
        } catch (Exception e) {
            logger.warn("Error unlocking repo for target {}", getId());
        }

        MDC.remove(TARGET_ID_MDC_KEY);
    }

    protected Collection<TargetLifecycleHook> getCreateHooks() throws ConfigurationException, DeployerException {
        return getHooksFromConfig(CREATE_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY);
    }

    protected Collection<TargetLifecycleHook> getDuplicateHooks() throws ConfigurationException, DeployerException {
        return getHooksFromConfig(DUPLICATE_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY);
    }

    protected Collection<TargetLifecycleHook> getInitHooks() throws DeployerException, ConfigurationException {
        return getHooksFromConfig(INIT_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY);
    }

    protected Collection<TargetLifecycleHook> getDeleteHooks() throws DeployerException, ConfigurationException {
        return getHooksFromConfig(DELETE_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY);
    }

    private Collection<TargetLifecycleHook> getHooksFromConfig(final String configKey) throws ConfigurationException, DeployerException {
        return targetLifecycleHooksResolver.getHooks(configuration, applicationContext, configKey);
    }

    protected void scheduleDeployments() throws ConfigurationException {
        boolean enabled = getBooleanProperty(configuration, TARGET_SCHEDULED_DEPLOYMENT_ENABLED_CONFIG_KEY, true);
        String cron = getStringProperty(configuration, TARGET_SCHEDULED_DEPLOYMENT_CRON_CONFIG_KEY);

        if (enabled && StringUtils.isNotEmpty(cron)) {
            logger.info("Deployments for target '{}' scheduled with cron {}", getId(), cron);

            scheduledDeploymentFuture = scheduler.schedule(new ScheduledDeploymentTask(), new CronTrigger(cron));
        }
    }

    protected void stopDeployments() {
        if (currentDeployment != null) {
            currentDeployment.end(Deployment.Status.INTERRUPTED);
        }

        if (CollectionUtils.isNotEmpty(pendingDeployments)) {
            Deployment deployment;

            while ((deployment = pendingDeployments.poll()) != null) {
                deployment.end(Deployment.Status.INTERRUPTED);
            }
        }
    }

    protected class ScheduledDeploymentTask implements Runnable {

        protected volatile Future<?> future;

        @Override
        public void run() {
            if (status == Status.INIT_COMPLETED) {
                if (future == null || future.isDone() && currentDeployment == null) {
                    pendingDeployments.add(new Deployment(TargetImpl.this));

                    future = executor.submit(new DeploymentTask());
                } else {
                    logger.info("Active deployment detected, skipping scheduled deployment for target {}", getId());
                }
            } else {
                logger.info("The target is not ready yet for deployments (status {})", status);
            }
        }

    }

    protected class DeploymentTask implements Runnable {

        @Override
        public void run() {
            MDC.put(TARGET_ID_MDC_KEY, getId());

            deploymentLock.lock();

            try {
                if (status == Status.INIT_COMPLETED) {
                    currentDeployment = pendingDeployments.poll();
                    TargetImpl.setCurrent(currentDeployment.getTarget());

                    if (currentDeployment != null && currentDeployment.getEnd() == null) {
                        logger.info("============================================================");
                        logger.info("Deployment for {} started", getId());
                        logger.info("============================================================");

                        try {
                            deploymentPipeline.execute(currentDeployment);
                        } finally {
                            double durationInSecs = currentDeployment.getDuration() / 1000.0;
                            String durationStr = String.format("%.3f", durationInSecs);

                            logger.info("============================================================");
                            logger.info("Deployment for {} finished in {} secs", getId(), durationStr);
                            logger.info("============================================================");
                        }
                    }
                }
            } finally {
                deploymentLock.unlock();

                currentDeployment = null;

                TargetImpl.clear();

                MDC.remove(TARGET_ID_MDC_KEY);
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

        TargetImpl target = (TargetImpl) o;

        return env.equals(target.env) &&
               siteName.equals(target.siteName) &&
               configurationFile.equals(target.configurationFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(env, siteName, configurationFile);
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
