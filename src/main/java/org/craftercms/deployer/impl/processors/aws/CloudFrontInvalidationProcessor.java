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
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.CreateInvalidationResult;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import org.springframework.web.util.UriUtils;

import static org.craftercms.deployer.utils.ConfigUtils.*;

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
     * Helper class the configures credentials and other properties for a {@link AmazonCloudFront} client.
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

        AmazonCloudFront client = buildClient();

        List<String> changedFiles =
            ListUtils.union(filteredChangeSet.getUpdatedFiles(), filteredChangeSet.getDeletedFiles());

        if (CollectionUtils.isNotEmpty(changedFiles)) {
            changedFiles = changedFiles.stream()
                                       .map(f -> UriUtils.encodePath(f, StandardCharsets.UTF_8))
                                       .collect(Collectors.toList());

            Paths paths = new Paths().withItems(changedFiles).withQuantity(changedFiles.size());

            logger.info("Will invalidate {} files", changedFiles.size());

            for (String distribution : distributions) {
                try {
                    String caller = UUID.randomUUID().toString();

                    logger.info("Creating invalidation for distribution {} with reference {}", distribution, caller);

                    InvalidationBatch batch = new InvalidationBatch().withPaths(paths).withCallerReference(caller);
                    CreateInvalidationRequest request = new CreateInvalidationRequest(distribution, batch);
                    CreateInvalidationResult result = client.createInvalidation(request);

                    logger.info("Created invalidation {} for distribution {}", result.getInvalidation().getId(),
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
     * Builds the {@link AmazonCloudFront} client.
     */
    protected AmazonCloudFront buildClient() {
        AmazonCloudFrontClientBuilder builder = AmazonCloudFrontClientBuilder.standard();
        builderConfigurer.configureClientBuilder(builder);

        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

}
