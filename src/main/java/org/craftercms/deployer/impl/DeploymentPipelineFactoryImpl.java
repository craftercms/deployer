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
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSOR_NAME_CONFIG_KEY;
import static org.craftercms.deployer.utils.ConfigUtils.getRequiredConfigurationsAt;

/**
 * Default implementation of {@link DeploymentPipeline}.
 *
 * @author avasquez
 */
@Component("deploymentPipelineFactory")
public class DeploymentPipelineFactoryImpl implements DeploymentPipelineFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentPipelineFactoryImpl.class);

    @Override
    public DeploymentPipeline getPipeline(HierarchicalConfiguration<ImmutableNode> configuration,
                                          ApplicationContext applicationContext, String pipelinePropertyName)
            throws ConfigurationException, DeployerException {
        List<HierarchicalConfiguration<ImmutableNode>> processorConfigs =
                getRequiredConfigurationsAt(configuration, pipelinePropertyName);
        List<DeploymentProcessor> deploymentProcessors = new ArrayList<>();

        for (HierarchicalConfiguration processorConfig : processorConfigs) {
            String processorName = getRequiredStringProperty(processorConfig, PROCESSOR_NAME_CONFIG_KEY);

            logger.debug("Initializing pipeline processor '{}'", processorName);

            try {
                DeploymentProcessor processor = applicationContext.getBean(processorName, DeploymentProcessor.class);
                processor.init(processorConfig);

                deploymentProcessors.add(processor);
            } catch (NoSuchBeanDefinitionException e) {
                throw new DeployerException("No processor bean found with name '" + processorName + "'", e);
            } catch (Exception e) {
                throw new DeployerException("Failed to initialize pipeline processor '" + processorName + "'", e);
            }
        }

        // Check that no main deployment processor is defined after a post deployment processor
        boolean postProcessorFound = false;
        for (DeploymentProcessor processor : deploymentProcessors) {
            if (!processor.isPostDeployment() && postProcessorFound) {
                throw new DeployerException("Processor " + processor + " can't be defined after a post processor " +
                                            "has already being defined");
            } else if (processor.isPostDeployment()) {
                postProcessorFound = true;
            }
        }

        return new DeploymentPipelineImpl(deploymentProcessors);
    }

}
