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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSOR_NAME_CONFIG_KEY;

/**
 * Default implementation of {@link DeploymentPipeline}.
 *
 * @author avasquez
 */
@Component("deploymentPipelineFactory")
public class DeploymentPipelineFactoryImpl implements DeploymentPipelineFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentPipelineFactoryImpl.class);

    @Override
    public DeploymentPipeline getPipeline(HierarchicalConfiguration configuration, ApplicationContext applicationContext,
                                          String pipelinePropertyName) throws DeployerException {
        List<HierarchicalConfiguration> processorConfigs = ConfigUtils.getRequiredConfigurationsAt(configuration, pipelinePropertyName);
        List<DeploymentProcessor> deploymentProcessors = new ArrayList<>();

        for (HierarchicalConfiguration processorConfig : processorConfigs) {
            String processorName = ConfigUtils.getRequiredStringProperty(processorConfig, PROCESSOR_NAME_CONFIG_KEY);

            logger.debug("Initializing pipeline processor '{}'", processorName);

            try {
                DeploymentProcessor processor = applicationContext.getBean(processorName, DeploymentProcessor.class);
                processor.init(processorConfig);

                deploymentProcessors.add(processor);
            } catch (NoSuchBeanDefinitionException e) {
                throw new DeployerException("No processor prototype bean found with name '" + processorName + "'", e);
            } catch (Exception e) {
                throw new DeployerException("Failed to initialize pipeline processor '" + processorName + "'", e);
            }
        }

        return new DeploymentPipelineImpl(deploymentProcessors);
    }

}
