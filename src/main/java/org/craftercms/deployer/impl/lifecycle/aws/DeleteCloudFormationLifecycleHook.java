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
package org.craftercms.deployer.impl.lifecycle.aws;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.*;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.craftercms.deployer.utils.aws.AwsCloudFormationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link TargetLifecycleHook} that deletes a CloudFormation stack.
 *
 * @author avasquez
 */
public class DeleteCloudFormationLifecycleHook implements TargetLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(DeleteCloudFormationLifecycleHook.class);

    protected static final String CONFIG_KEY_STACK_NAME = "stackName";

    // Config properties (populated on init)

    protected AwsClientBuilderConfigurer builderConfigurer;
    protected String stackName;

    @Override
    public void init(Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        stackName = getRequiredStringProperty(config, CONFIG_KEY_STACK_NAME);
    }

    @Override
    public void execute(Target target) throws DeployerException {
        AmazonCloudFormation cloudFormation = AwsCloudFormationUtils.buildClient(builderConfigurer);

        if (AwsCloudFormationUtils.stackExists(cloudFormation, stackName)) {
            logger.info("Deleting CloudFormation stack '{}'", stackName);

            try {
                cloudFormation.deleteStack(new DeleteStackRequest().withStackName(stackName));

                logger.info("Deletion of CloudFormation stack '{}' started", stackName);
            } catch (Exception e) {
                throw new DeployerException("Error while deleting CloudFormation stack '" + stackName + "'", e);
            }
        } else {
            logger.info("CloudFormation stack '{}' doesn't exist. Skipping delete...", stackName);
        }
    }

}
