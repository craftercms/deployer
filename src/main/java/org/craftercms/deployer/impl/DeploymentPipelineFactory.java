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

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.springframework.context.ApplicationContext;

/**
 * Factory that uses target-specific YAML configuration and Spring configuration to a create the {@link DeploymentPipeline} for a
 * target.
 *
 * @author avasquez
 */
public interface DeploymentPipelineFactory {

    /**
     * Creates a {@link DeploymentPipeline} based on the specified configuration
     *
     * @param configuration         the target's YAML configuration
     * @param applicationContext    the target's application context
     * @param pipelinePropertyName  the name of the pipeline property in the YAML configuration
     *
     * @return the deployment pipeline
     *
     * @throws ConfigurationException if a configuration related exception occurs
     * @throws DeployerException if a general error occurs
     */
    DeploymentPipeline getPipeline(HierarchicalConfiguration configuration, ApplicationContext applicationContext,
                                   String pipelinePropertyName) throws ConfigurationException, DeployerException;

}
