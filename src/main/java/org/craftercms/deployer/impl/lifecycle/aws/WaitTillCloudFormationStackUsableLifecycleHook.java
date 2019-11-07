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
import com.amazonaws.services.cloudformation.model.Output;
import com.amazonaws.services.cloudformation.model.Stack;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.craftercms.deployer.utils.aws.AwsCloudFormationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.craftercms.commons.config.ConfigUtils.getIntegerProperty;
import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * {@link TargetLifecycleHook} that waits until a CloudFormation stack is usable, and then maps the outputs of the
 * stack to target configuration properties.
 *
 * @author avasquez
 */
public class WaitTillCloudFormationStackUsableLifecycleHook implements TargetLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(WaitTillCloudFormationStackUsableLifecycleHook.class);

    public static final int DEFAULT_SECONDS_BEFORE_CHECKING_STATUS = 60;

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
    protected static final String CONFIG_KEY_SECONDS_BEFORE_CHECKING_STATUS = "secondsBeforeCheckingStatus";

    protected Configuration targetConfig;

    // Config properties (populated on init)

    protected AwsClientBuilderConfigurer builderConfigurer;
    protected String stackName;
    protected Map<String, String> outputMappings;
    protected int secondsBeforeCheckingStatus;

    public WaitTillCloudFormationStackUsableLifecycleHook() {
        this.outputMappings = new HashMap<>();
    }

    @Required
    public void setTargetConfig(Configuration targetConfig) {
        this.targetConfig = targetConfig;
    }

    @Override
    public void init(Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        stackName = getRequiredStringProperty(config, CONFIG_KEY_STACK_NAME);
        secondsBeforeCheckingStatus = getIntegerProperty(config, CONFIG_KEY_SECONDS_BEFORE_CHECKING_STATUS,
                                                         DEFAULT_SECONDS_BEFORE_CHECKING_STATUS);

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
    public void execute(Target target) throws DeployerException {
        AmazonCloudFormation cloudFormation = AwsCloudFormationUtils.buildClient(builderConfigurer);

        while (!isTargetDeleted(target) && !isStackUsable(cloudFormation)) {
            try {
                Thread.sleep(secondsBeforeCheckingStatus * 1000);
            } catch (InterruptedException e) {
                logger.debug(
                        "Thread interrupted while waiting to check again for CloudFormation stack '{}' status", stackName);
            }
        }
    }

    protected boolean isStackUsable(AmazonCloudFormation cloudFormation) throws DeployerException {
        Stack stack = AwsCloudFormationUtils.getStack(cloudFormation, stackName);
        if (stack != null) {
            String statusCode = stack.getStackStatus();
            if (ArrayUtils.contains(STACK_STATUS_CODES_USABLE, statusCode)) {
                logger.info("CloudFormation stack '{}' is usable (status '{}')", stackName, statusCode);

                mapOutputsToConfig(stack.getOutputs());

                return true;
            } else if (ArrayUtils.contains(STACK_STATUS_CODES_IN_PROGRESS, statusCode)) {

                logger.info("CloudFormation stack '" + stackName + "' is not yet usable because there's an " +
                            "operation in progress (status '" + statusCode + "')");

                return false;
            } else {
                throw new DeployerException("CloudFormation stack '" + stackName + "' is in an unusable state " +
                                            "(status '" + statusCode + ")");
            }
        } else {
            throw new DeployerException("CloudFormation stack '" + stackName + "' doesn't exist");
        }
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

    protected boolean isTargetDeleted(Target target) {
        return target.getStatus() == Target.Status.DELETE_IN_PROGRESS || target.getStatus() == Target.Status.DELETED;
    }

}
