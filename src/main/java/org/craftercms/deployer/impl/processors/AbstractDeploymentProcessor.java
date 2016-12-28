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

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.craftercms.deployer.impl.CommonConfigurationProperties.*;

/**
 * Created by alfonsovasquez on 12/27/16.
 */
public abstract class AbstractDeploymentProcessor implements DeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDeploymentProcessor.class);

    protected String deploymentId;
    protected String processorName;

    @Override
    public void init(Configuration mainConfig, Configuration processorConfig) throws DeploymentException {
        deploymentId = ConfigurationUtils.getRequiredString(mainConfig, DEPLOYMENT_ID_PROPERTY_NAME);
        processorName = ConfigurationUtils.getRequiredString(processorConfig, PROCESSOR_NAME_PROPERTY_NAME);

        doInit(mainConfig, processorConfig);
    }

    @Override
    public ChangeSet execute(DeploymentContext context, ChangeSet changeSet) throws DeploymentException {
        try {
            logger.info("========== Start of processor '{}' for deployment '{}' ==========", processorName, deploymentId);

            return doExecute(context, changeSet);
        } finally {
            logger.info("=========== End of processor '{}' for deployment '{}' ===========", processorName, deploymentId);
        }
    }

    protected abstract void doInit(Configuration mainConfig, Configuration processorConfig) throws DeploymentException;

    protected abstract ChangeSet doExecute(DeploymentContext context, ChangeSet changeSet) throws DeploymentException;

}
