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
package org.craftercms.deployer.impl.processors.aws;

import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.craftercms.deployer.utils.aws.AwsCloudFormationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Processor that verifies the status of a CloudFormation stack. If the status indicates the stack is usable,
 * deployment continues and the outputs of the stack are mapped to target configuration properties that can be used
 * by other processors. If the stack has an operation in progress (most of the time, create), the deployment
 * will be finished immediately so a future deployment can check again if the stack is ready. If the stack is
 * in an unusable state, and exception is thrown to indicate that any issues should be fixed before continuing.
 *
 * @author avasquez
 */
public class VerifyCloudFormationStatusProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(VerifyCloudFormationStatusProcessor.class);

    protected static final String[] STACK_STATUS_CODES_USABLE = {
            "CREATE_COMPLETE",
            "UPDATE_COMPLETE",
            "UPDATE_COMPLETE_CLEANUP_IN_PROGRESS",
            "UPDATE_ROLLBACK_COMPLETE",
            "UPDATE_ROLLBACK_COMPLETE_CLEANUP_IN_PROGRESS"
    };

    protected static final String[] STACK_STATUS_CODES_IN_PROGRESS = {
            "CREATE_IN_PROGRESS",
            "REVIEW_IN_PROGRESS",
            "UPDATE_IN_PROGRESS",
            "UPDATE_ROLLBACK_IN_PROGRESS"
    };

    protected static final String CONFIG_KEY_STACK_NAME = "stackName";
    protected static final String CONFIG_KEY_OUTPUT_MAPPINGS = "outputMappings";

    protected Configuration targetConfig;

    // Config properties (populated on init)

    protected AwsClientBuilderConfigurer builderConfigurer;
    protected String stackName;
    protected Map<String, String> outputMappings;

    public VerifyCloudFormationStatusProcessor() {
        this.outputMappings = new HashMap<>();
    }

    @Required
    public void setTargetConfig(Configuration targetConfig) {
        this.targetConfig = targetConfig;
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running
        return deployment.isRunning();
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        stackName = getRequiredStringProperty(config, CONFIG_KEY_STACK_NAME);

        Configuration outputMappingsConfig = config.subset(CONFIG_KEY_OUTPUT_MAPPINGS);
        if (outputMappingsConfig != null) {
            Iterator<String> keys = outputMappingsConfig.getKeys();
            while (keys.hasNext()) {
                String outputKey = keys.next();
                String configKey = outputMappingsConfig.getString(outputKey);

                outputMappings.put(outputKey, configKey);
            }
        }
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        AmazonCloudFormation cloudFormation = AwsCloudFormationUtils.buildClient(builderConfigurer);
        Stack stack = AwsCloudFormationUtils.getStack(cloudFormation, stackName);

        if (stack != null) {
            String statusCode = stack.getStackStatus();
            if (ArrayUtils.contains(STACK_STATUS_CODES_USABLE, statusCode)) {
                logger.info("CloudFormation stack '{}' is usable (status '{}')", stackName, statusCode);

                mapOutputsToConfig(stack.getOutputs());
            } else if (ArrayUtils.contains(STACK_STATUS_CODES_IN_PROGRESS, statusCode)) {
                String msg = "CloudFormation stack '" + stackName + "' is not yet usable because there's an " +
                             "operation in progress (status '" + statusCode + "'). This deployment will end " +
                             "immediately, and next deployment will check again if the stack is ready";

                logger.info(msg);

                execution.setStatusDetails(msg);
                deployment.end(Deployment.Status.SUCCESS);
            } else {
                throw new DeployerException("CloudFormation stack '" + stackName + "' is in an unusable state " +
                                            "(status '" + statusCode + "), probably caused by an operation failure " +
                                            ". Deployments can't continue until all issues are fixed");
            }
        } else {
            throw new DeployerException("CloudFormation stack '" + stackName + "' doesn't exist");
        }

        return null;
    }

    protected void mapOutputsToConfig(List<Output> outputs) {
        if (CollectionUtils.isNotEmpty(outputs)) {
            for (Output output : outputs) {
                String configKey = outputMappings.get(output.getOutputKey());
                String outputValue = output.getOutputValue();

                targetConfig.setProperty(configKey, outputValue);
            }
        }
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

}
