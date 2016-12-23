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
package org.craftercms.deployer.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeploymentConfigurationException;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.stereotype.Component;

/**
 * Created by alfonsovasquez on 12/22/16.
 */
@Component("deploymentPipelineFactory")
public class DeploymentPipelineFactoryImpl implements DeploymentPipelineFactory {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentPipelineFactoryImpl.class);

    public static final String PIPELINE_PROPERTY_NAME = "deployer.pipeline";
    public static final String PROCESSOR_NAME_PROPERTY_NAME = "processorName";

    @Override
    public DeploymentPipeline getPipeline(HierarchicalConfiguration configuration,
                                          BeanFactory beanFactory) throws DeploymentException {
        List<HierarchicalConfiguration> processorConfigs = ConfigurationUtils.configurationsAt(configuration, PIPELINE_PROPERTY_NAME, true);
        List<DeploymentProcessor> processors = new ArrayList<>(processorConfigs.size());

        for (HierarchicalConfiguration processorConfig : processorConfigs) {
            String processorName = ConfigurationUtils.getString(processorConfig, PROCESSOR_NAME_PROPERTY_NAME, true);
            DeploymentProcessor processor;

            logger.debug("Initializing pipeline processor '{}'", processorName);

            try {
                processor = beanFactory.getBean(processorName, DeploymentProcessor.class);
                processor.init(processorConfig);
            } catch (NoSuchBeanDefinitionException e) {
                throw new DeploymentConfigurationException("No processor prototype bean found with name '" + processorName + "'", e);
            } catch (Exception e) {
                throw new DeploymentConfigurationException("Failed to initialize pipeline processor '" + processorName + "'", e);
            }

            processors.add(processor);
        }

        return new DeploymentPipelineImpl(processors);
    }

}
