/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.cluster.ClusterMode;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

import static org.craftercms.deployer.api.cluster.ClusterMode.UNKNOWN;

/**
 * Default implementation of {@link DeploymentPipeline}.
 *
 * @author avasquez
 */
public class DeploymentPipelineImpl implements DeploymentPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    protected final List<DeploymentProcessor> deploymentProcessors;

    protected final boolean clusterOn;

    public DeploymentPipelineImpl(List<DeploymentProcessor> deploymentProcessors, boolean clusterOn) {
        this.deploymentProcessors = deploymentProcessors;
        this.clusterOn = clusterOn;
    }

    @Override
    public void destroy() throws DeployerException {
        if (CollectionUtils.isNotEmpty(deploymentProcessors)) {
            for (DeploymentProcessor processor : deploymentProcessors) {
                try {
                    processor.destroy();
                } catch (Exception e) {
                    logger.error("Failed to destroy processor " + processor, e);
                }
            }
        }
    }

    @Override
    public List<DeploymentProcessor> getProcessors() {
        return Collections.unmodifiableList(deploymentProcessors);
    }

    @Override
    public void execute(Deployment deployment) {
        deployment.start();
        try {
            executeProcessors(deployment);

            deployment.end(Deployment.Status.SUCCESS);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while executing deployment pipeline for target '{}'",
                         deployment.getTarget().getId(), e);

            deployment.end(Deployment.Status.FAILURE);
        }
    }

    protected void executeProcessors(Deployment deployment) {
        if (!CollectionUtils.isNotEmpty(deploymentProcessors)) {
            return;
        }
        ClusterMode clusterMode = deployment.getClusterMode();
        if (clusterOn && UNKNOWN == clusterMode) {
            logger.info("Cluster mode is unknown, will not run any processors");
            return;
        }
        for (DeploymentProcessor processor : deploymentProcessors) {
            if (processor.supportsMode(deployment.getMode()) &&
                    (!clusterOn || processor.supportsClusterMode(clusterMode))) {
                processor.execute(deployment);
            }
        }
    }

}
