/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link DeploymentPipeline}.
 *
 * @author avasquez
 */
public class DeploymentPipelineImpl implements DeploymentPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    protected List<DeploymentProcessor> deploymentProcessors;

    public DeploymentPipelineImpl(List<DeploymentProcessor> deploymentProcessors) {
        this.deploymentProcessors = deploymentProcessors;
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
        if (CollectionUtils.isNotEmpty(deploymentProcessors)) {
            for (DeploymentProcessor processor : deploymentProcessors) {
                processor.execute(deployment);
            }
        }
    }

}
