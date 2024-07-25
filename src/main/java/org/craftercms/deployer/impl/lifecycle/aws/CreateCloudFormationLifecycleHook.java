/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.impl.lifecycle.AbstractLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.craftercms.deployer.utils.aws.AwsCloudFormationUtils;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.model.Capability;
import software.amazon.awssdk.services.cloudformation.model.CreateStackRequest;
import software.amazon.awssdk.services.cloudformation.model.CreateStackResponse;
import software.amazon.awssdk.services.cloudformation.model.Parameter;

import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Implementation of {@link TargetLifecycleHook} that creates a CloudFormation stack based on a provided
 * CloudFormation template file.
 *
 * @author avasquez
 */
public class CreateCloudFormationLifecycleHook extends AbstractLifecycleHook {

    protected static final String CONFIG_KEY_STACK_NAME = "stackName";
    protected static final String CONFIG_KEY_TEMPLATE_FILENAME = "templateFilename";
    protected static final String CONFIG_KEY_TEMPLATE_PARAMS = "templateParams";
    protected static final String CONFIG_KEY_STACK_CAPABILITIES = "stackCapabilities";

    protected Resource templatesResource;
    protected Resource templatesOverrideResource;

    // Config properties (populated on init)

    protected AwsClientBuilderConfigurer builderConfigurer;
    protected String stackName;
    protected String templateFilename;
    protected Collection<Parameter> templateParams;
    protected List<Capability> stackCapabilities;

    public CreateCloudFormationLifecycleHook() {
        this.templateParams = new ArrayList<>();
        this.stackCapabilities = new ArrayList<>();
    }

    public void setTemplatesResource(Resource templatesResource) {
        this.templatesResource = templatesResource;
    }

    public void setTemplatesOverrideResource(Resource templatesOverrideResource) {
        this.templatesOverrideResource = templatesOverrideResource;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void doInit(Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        stackName = getRequiredStringProperty(config, CONFIG_KEY_STACK_NAME);
        templateFilename = getRequiredStringProperty(config, CONFIG_KEY_TEMPLATE_FILENAME);

        Configuration templateParamsConfig = config.subset(CONFIG_KEY_TEMPLATE_PARAMS);
        if (templateParamsConfig != null) {
            Iterator<String> keys = templateParamsConfig.getKeys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = templateParamsConfig.getString(key);

                templateParams.add(Parameter.builder()
                        .parameterKey(key)
                        .parameterValue(value)
                        .build());
            }
        }

        this.stackCapabilities = loadCapabilities(config);
    }

    /**
     * Load capabilities from configuration
     * @param config the configuration object
     * @return list of {@link Capability}
     * @throws ConfigurationException
     */
    private List<Capability> loadCapabilities(Configuration config) throws ConfigurationException {
        String property = getStringProperty(config, CONFIG_KEY_STACK_CAPABILITIES, "");
        if (isEmpty(property)) {
            return new ArrayList<>();
        }

        return Arrays.stream(property.split(","))
                .map(String::trim)
                .map(Capability::fromValue)
                .collect(Collectors.toList());
    }

    @Override
    public void doExecute(Target target) throws DeployerException {
        CloudFormationClient cloudFormation = AwsCloudFormationUtils.buildClient(builderConfigurer);

        if (AwsCloudFormationUtils.stackExists(cloudFormation, stackName)) {
            logger.info("CloudFormation stack '{}' already exists. Skipping create...", stackName);
        } else {
            logger.info("Creating CloudFormation stack '{}'", stackName);

            createCloudFormationStack(cloudFormation);
        }
    }

    protected void createCloudFormationStack(CloudFormationClient cloudFormation) throws DeployerException {
        try {
            CreateStackRequest.Builder builder = CreateStackRequest.builder()
                    .stackName(stackName)
                    .templateBody(getTemplateBody())
                    .parameters(templateParams);

            if (isNotEmpty(stackCapabilities)) {
                builder.capabilities(stackCapabilities);
            }

            CreateStackRequest request = builder.build();

            CreateStackResponse result = cloudFormation.createStack(request);

            logger.info("Creation of CloudFormation stack '{}' started (stack ID '{}')", stackName, result.stackId());
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
