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
package org.craftercms.deployer.impl.processors;

import java.time.ZonedDateTime;

import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alfonsovasquez on 12/27/16.
 */
public abstract class AbstractMainDeploymentProcessor extends AbstractDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMainDeploymentProcessor.class);

    @Override
    public void execute(Deployment deployment) {
        if (shouldExecute(deployment)) {
            ProcessorExecution execution = new ProcessorExecution(processorName);

            deployment.addProcessorExecution(execution);

            try {
                logger.info("========== Start of '{}' for target '{}' ==========", processorName, targetId);

                doExecute(deployment, execution);

                if (execution.getStatus() == null) {
                    execution.setStatus(Deployment.Status.SUCCESS);
                }
            } catch (Exception e) {
                logger.error("Processor '" + processorName + "' for target '" + targetId + "' failed", e);

                execution.setStatus(Deployment.Status.FAILURE);
                execution.setStatusDetails(e.toString());

                if (failDeploymentOnProcessorFailure()) {
                    deployment.setStatus(Deployment.Status.FAILURE);
                }
            } finally {
                execution.setRunning(false);
                if (deployment.getEnd() == null) {
                    execution.setEnd(ZonedDateTime.now());
                }

                logger.info("=========== End of '{}' for target '{}' ===========", processorName, targetId);
            }
        }
    }

    protected boolean shouldExecute(Deployment deployment) {
        // Don't run if the deployment already failed or if the deployment change set is empty
        return deployment.getStatus() != Deployment.Status.FAILURE && !deployment.isChangeSetEmpty();
    }

    protected abstract void doExecute(Deployment deployment, ProcessorExecution execution) throws DeploymentException;

    protected abstract boolean failDeploymentOnProcessorFailure();

}
