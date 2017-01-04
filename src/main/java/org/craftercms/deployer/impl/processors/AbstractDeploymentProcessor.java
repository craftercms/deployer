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
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;

import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSOR_NAME_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_ID_CONFIG_KEY;

/**
 * Created by alfonsovasquez on 12/30/16.
 */
public abstract class AbstractDeploymentProcessor implements DeploymentProcessor {

    protected String targetId;
    protected String processorName;

    @Override
    public void init(Configuration mainConfig, Configuration processorConfig) throws DeploymentException {
        targetId = ConfigurationUtils.getRequiredString(mainConfig, TARGET_ID_CONFIG_KEY);
        processorName = ConfigurationUtils.getRequiredString(processorConfig, PROCESSOR_NAME_CONFIG_KEY);

        doInit(mainConfig, processorConfig);
    }

    @Override
    public void destroy() throws DeploymentException {
    }

    protected abstract void doInit(Configuration mainConfig, Configuration processorConfig) throws DeploymentException;

}
