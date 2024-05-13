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
package org.craftercms.deployer.utils.aws;

import org.apache.commons.collections.CollectionUtils;
import org.craftercms.deployer.api.exceptions.DeployerException;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudformation.CloudFormationClientBuilder;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksRequest;
import software.amazon.awssdk.services.cloudformation.model.DescribeStacksResponse;
import software.amazon.awssdk.services.cloudformation.model.Stack;

import java.util.List;

/**
 * Utility methods for AWS CloudFormation.
 *
 * @author avasquez
 */
public class AwsCloudFormationUtils {

    private AwsCloudFormationUtils() {
    }

    /**
     * Builds an {@code AmazonCloudFormation} client, using the provided {@link AwsClientBuilderConfigurer}.
     *
     * @param builderConfigurer the helper used to configure the {@code AmazonCloudFormationClientBuilder}
     * @return the built {@code AmazonCloudFormation} client
     */
    public static CloudFormationClient buildClient(AwsClientBuilderConfigurer builderConfigurer) {
        CloudFormationClientBuilder builder = CloudFormationClient.builder();
        builderConfigurer.configureClientBuilder(builder);

        return builder.build();
    }

    /**
     * Returns true if the specified stack exists, false otherwise.
     *
     * @param cloudFormation the CloudFormation client
     * @param stackName the stack name
     *
     * @return true if the specified stack exists, false otherwise.
     * @throws DeployerException in an error occurs
     */
    public static boolean stackExists(CloudFormationClient cloudFormation, String stackName) throws DeployerException {
        return getStack(cloudFormation, stackName) != null;
    }

    /**
     * Returns the info of the specifed stack.
     *
     * @param cloudFormation the CloudFormation client
     * @param stackName the stack name
     *
     * @return the info of the stack
     * @throws DeployerException if an error occurs
     */
    public static Stack getStack(CloudFormationClient cloudFormation, String stackName) throws DeployerException {
        try {
            DescribeStacksRequest request = DescribeStacksRequest.builder()
                    .stackName(stackName)
                    .build();
            DescribeStacksResponse result = cloudFormation.describeStacks(request);
            List<Stack> stacks = result.stacks();

            if (CollectionUtils.isNotEmpty(stacks)) {
                return stacks.get(0);
            } else {
                // Shouldn't happen, AWS throws an exception if stack doesn't exist
                return null;
            }
        } catch (Exception e) {
            // HORRIBLE, but only way to know if stack doesn't exist
            if (e.getMessage().matches("(.*)" + stackName + "(.*)does not exist(.*)")) {
                return null;
            } else {
                throw new DeployerException("Error while getting CloudFormation stack " + stackName, e);
            }
        }
    }

}
