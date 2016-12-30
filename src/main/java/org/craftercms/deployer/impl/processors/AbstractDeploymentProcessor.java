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

import java.time.Instant;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.TargetContext;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.craftercms.deployer.impl.CommonConfigurationProperties.EXECUTE_ON_EMPTY_CHANGE_SET_PROPERTY_NAME;
import static org.craftercms.deployer.impl.CommonConfigurationProperties.PROCESSOR_NAME_PROPERTY_NAME;
import static org.craftercms.deployer.impl.CommonConfigurationProperties.TARGET_ID_PROPERTY_NAME;

/**
 * Created by alfonsovasquez on 12/27/16.
 */
public abstract class AbstractDeploymentProcessor implements DeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDeploymentProcessor.class);

    protected String targetId;
    protected String processorName;
    protected boolean executeOnEmptyChangeSet;

    @Override
    public void init(Configuration mainConfig, Configuration processorConfig) throws DeploymentException {
        targetId = ConfigurationUtils.getRequiredString(mainConfig, TARGET_ID_PROPERTY_NAME);
        processorName = ConfigurationUtils.getRequiredString(processorConfig, PROCESSOR_NAME_PROPERTY_NAME);
        executeOnEmptyChangeSet = ConfigurationUtils.getBoolean(processorConfig, EXECUTE_ON_EMPTY_CHANGE_SET_PROPERTY_NAME, false);

        doInit(mainConfig, processorConfig);
    }

    @Override
    public void execute(Deployment deployment, TargetContext context) {
        if (deployment.getStatus() != Deployment.Status.FAILURE && (!deployment.isChangeSetEmpty() || executeOnEmptyChangeSet)) {
            ProcessorExecution execution = new ProcessorExecution(processorName);

            deployment.addProcessorExecution(execution);

            try {
                logger.info("========== Start of processor '{}' for target '{}' ==========", processorName, targetId);

                doExecute(deployment, execution, context);

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
                execution.setEnd(Instant.now());
                execution.setRunning(false);

                logger.info("=========== End of processor '{}' for target '{}' ===========", processorName, targetId);
            }
        }
    }

    protected abstract void doInit(Configuration mainConfig, Configuration processorConfig) throws DeploymentException;

    protected abstract void doExecute(Deployment deployment, ProcessorExecution execution,
                                      TargetContext context) throws DeploymentException;

    protected abstract boolean failDeploymentOnProcessorFailure();

}
