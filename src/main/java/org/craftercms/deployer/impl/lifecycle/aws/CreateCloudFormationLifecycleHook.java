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
import com.amazonaws.services.cloudformation.model.CreateStackRequest;
import com.amazonaws.services.cloudformation.model.CreateStackResult;
import com.amazonaws.services.cloudformation.model.Parameter;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link TargetLifecycleHook} that creates a CloudFormation stack based on a provided
 * CloudFormation template file.
 *
 * @author avasquez
 */
public class CreateCloudFormationLifecycleHook implements TargetLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(CreateCloudFormationLifecycleHook.class);

    protected static final String CONFIG_KEY_STACK_NAME = "stackName";
    protected static final String CONFIG_KEY_TEMPLATE_FILENAME = "templateFilename";
    protected static final String CONFIG_KEY_TEMPLATE_PARAMS = "templateParams";

    protected Resource templatesResource;
    protected Resource templatesOverrideResource;

    // Config properties (populated on init)

    protected AwsClientBuilderConfigurer builderConfigurer;
    protected String stackName;
    protected String templateFilename;
    protected Collection<Parameter> templateParams;

    public CreateCloudFormationLifecycleHook() {
        this.templateParams = new ArrayList<>();
    }

    @Required
    public void setTemplatesResource(Resource templatesResource) {
        this.templatesResource = templatesResource;
    }

    @Required
    public void setTemplatesOverrideResource(Resource templatesOverrideResource) {
        this.templatesOverrideResource = templatesOverrideResource;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init(Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        stackName = getRequiredStringProperty(config, CONFIG_KEY_STACK_NAME);
        templateFilename = getRequiredStringProperty(config, CONFIG_KEY_TEMPLATE_FILENAME);

        Configuration templateParamsConfig = config.subset(CONFIG_KEY_TEMPLATE_PARAMS);
        if (templateParamsConfig != null) {
            Iterator<String> keys = templateParamsConfig.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = templateParamsConfig.getString(key);

                templateParams.add(new Parameter().withParameterKey(key).withParameterValue(value));
            }
        }
    }

    @Override
    public void execute(Target target) throws DeployerException {
        AmazonCloudFormation cloudFormation = AwsCloudFormationUtils.buildClient(builderConfigurer);

        if (AwsCloudFormationUtils.stackExists(cloudFormation, stackName)) {
            logger.info("CloudFormation stack '{}' already exists. Skipping create...", stackName);
        } else {
            logger.info("Creating CloudFormation stack '{}'", stackName);

            createCloudFormationStack(cloudFormation);
        }
    }

    protected void createCloudFormationStack(AmazonCloudFormation cloudFormation) throws DeployerException {
        try {
            CreateStackRequest request = new CreateStackRequest();
            request.setStackName(stackName);
            request.setTemplateBody(getTemplateBody());
            request.setParameters(templateParams);

            CreateStackResult result = cloudFormation.createStack(request);

            logger.info("Creation of CloudFormation stack '{}' started (stack ID '{}')", stackName, result.getStackId());
        } catch (Exception e) {
            throw new DeployerException("Unable to create CloudFormation stack '" + stackName + "'", e);
        }
    }

    protected String getTemplateBody() throws DeployerException {
        try {
            Resource templateResource = templatesOverrideResource.createRelative(templateFilename);
            if (!templateResource.exists()) {
                templateResource = templatesResource.createRelative(templateFilename);
                if (!templateResource.exists()) {
                    throw new FileNotFoundException("Unable to resolve template " + templateFilename +
                                                    ". It wasn't found in " + templatesOverrideResource +
                                                    " or in " + templatesResource);
                }
            }

            return IOUtils.toString(templateResource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DeployerException("Error while retrieving body of template " + templateFilename, e);
        }
    }

}
