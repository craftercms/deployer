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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * {@link TargetLifecycleHook} that clears an S3 bucket. Mostly used in preparation for deleting a bucket
 * (most APIs don't allow deleting a non-empty bucket).
 *
 * @author avasquez
 */
public class ClearS3BucketLifecycleHook implements TargetLifecycleHook {

    private static final Logger logger = LoggerFactory.getLogger(ClearS3BucketLifecycleHook.class);

    protected static final String CONFIG_KEY_BUCKET_NAME = "bucketName";

    // Config properties (populated on init)

    protected AwsClientBuilderConfigurer builderConfigurer;
    protected String bucketName;

    @Override
    public void init(Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        bucketName = getRequiredStringProperty(config, CONFIG_KEY_BUCKET_NAME);
    }

    @Override
    public void execute(Target target) throws DeployerException {
        try {
            boolean done = false;
            AmazonS3 s3 = buildClient();

            if (s3.doesBucketExistV2(bucketName)) {
                ObjectListing listing = s3.listObjects(bucketName);

                logger.info("Clearing S3 bucket '{}'...", bucketName);

                while (!done) {
                    List<DeleteObjectsRequest.KeyVersion> objectsToDelete =
                            listing.getObjectSummaries().stream()
                                   .map(object -> new DeleteObjectsRequest.KeyVersion(object.getKey()))
                                   .collect(Collectors.toList());

                    logger.info("Deleting {} files", objectsToDelete.size());

                    s3.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(objectsToDelete));

                    if (listing.isTruncated()) {
                        listing = s3.listNextBatchOfObjects(listing);
                    } else {
                        done = true;
                    }
                }
            }
        } catch (Exception e) {
            throw new DeployerException("Error while trying to clear S3 bucket '" + bucketName + "'", e);
        }
    }

    protected AmazonS3 buildClient() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builderConfigurer.configureClientBuilder(builder);

        return builder.build();
    }

}
