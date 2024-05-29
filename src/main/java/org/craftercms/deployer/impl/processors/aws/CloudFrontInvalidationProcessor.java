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

package org.craftercms.deployer.impl.processors.aws;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import software.amazon.awssdk.services.cloudformation.CloudFormationClient;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.CloudFrontClientBuilder;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse;
import software.amazon.awssdk.services.cloudfront.model.InvalidationBatch;
import software.amazon.awssdk.services.cloudfront.model.Paths;

import static org.craftercms.commons.config.ConfigUtils.*;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} that invalidates the changed
 * files in the given AWS Cloudfront distributions.
 * Can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>distributions:</strong> List of distributions ids</li>
 * </ul>
 *
 * @author joseross
 */
public class CloudFrontInvalidationProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CloudFrontInvalidationProcessor.class);

    protected static final String CONFIG_KEY_DISTRIBUTIONS = "distributions";

    // Config properties (populated on init)

    /**
     * Helper class the configures credentials and other properties for a {@link CloudFrontClient} client.
     */
    protected AwsClientBuilderConfigurer builderConfigurer;
    /**
     * List of distribution ids
     */
    protected String[] distributions;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit(final Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        distributions = getRequiredStringArrayProperty(config, CONFIG_KEY_DISTRIBUTIONS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {

        logger.info("Performing Cloudfront invalidation...");

        CloudFrontClient client = buildClient();

        List<String> changedFiles =
            ListUtils.union(filteredChangeSet.getUpdatedFiles(), filteredChangeSet.getDeletedFiles());

        if (CollectionUtils.isNotEmpty(changedFiles)) {
            changedFiles = changedFiles.stream()
                                       .map(f -> UriUtils.encodePath(f, StandardCharsets.UTF_8))
                                       .collect(Collectors.toList());

            Paths paths = Paths.builder()
                    .items(changedFiles)
                    .quantity(changedFiles.size())
                    .build();

            logger.info("Will invalidate {} files", changedFiles.size());

            for (String distribution : distributions) {
                try {
                    String caller = UUID.randomUUID().toString();

                    logger.info("Creating invalidation for distribution {} with reference {}", distribution, caller);

                    InvalidationBatch batch = InvalidationBatch.builder()
                            .paths(paths)
                            .callerReference(caller)
                            .build();
                    CreateInvalidationRequest request = CreateInvalidationRequest.builder()
                            .distributionId(distribution)
                            .invalidationBatch(batch)
                            .build();
                    CreateInvalidationResponse result = client.createInvalidation(request);

                    logger.info("Created invalidation {} for distribution {}", result.invalidation().id(),
                                distribution);
                } catch (Exception e) {
                    throw new DeployerException("Error invalidating changed files for distribution " + distribution, e);
                }
            }
        } else {
            logger.info("No actual files that need to be invalidated");
        }

        return null;
    }

    /**
     * Builds the {@link CloudFrontClient} client.
     */
    protected CloudFrontClient buildClient() {
        CloudFrontClientBuilder builder = CloudFrontClient.builder();
        builderConfigurer.configureClientBuilder(builder);

        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

}
