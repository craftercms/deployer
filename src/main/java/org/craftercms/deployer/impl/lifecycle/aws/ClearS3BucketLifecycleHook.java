/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.VersionListing;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.impl.lifecycle.AbstractLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;

import java.util.List;
import java.util.stream.Collectors;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * {@link TargetLifecycleHook} that clears an S3 bucket. Mostly used in preparation for deleting a bucket
 * (most APIs don't allow deleting a non-empty bucket).
 *
 * @author avasquez
 */
public class ClearS3BucketLifecycleHook extends AbstractLifecycleHook {

    protected static final String CONFIG_KEY_BUCKET_NAME = "bucketName";

    // Config properties (populated on init)

    protected AwsClientBuilderConfigurer builderConfigurer;
    protected String bucketName;

    @Override
    public void doInit(Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        bucketName = getRequiredStringProperty(config, CONFIG_KEY_BUCKET_NAME);
    }

    @Override
    public void doExecute(Target target) throws DeployerException {
        try {
            AmazonS3 s3 = buildClient();

            if (s3.doesBucketExistV2(bucketName)) {
                logger.info("Emptying bucket '{}'...", bucketName);

                // Delete all objects from the bucket. This is sufficient
                // for unversioned buckets. For versioned buckets, when you attempt to delete objects, Amazon S3 inserts
                // delete markers for all objects, but doesn't delete the object versions.
                // To delete objects from versioned buckets, delete all of the object versions before deleting
                // the bucket (see below for an example).
                ObjectListing objectList = s3.listObjects(bucketName);
                while (true) {
                    List<DeleteObjectsRequest.KeyVersion> objectsToDelete = objectList.getObjectSummaries().stream()
                            .map(o -> new DeleteObjectsRequest.KeyVersion(o.getKey()))
                            .collect(Collectors.toList());

                    logger.info("Deleting {} objects", objectsToDelete.size());

                    s3.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(objectsToDelete));

                    // If the bucket contains many objects, the listObjects() call
                    // might not return all of the objects in the first listing. Check to
                    // see whether the listing was truncated. If so, retrieve the next page of objects 
                    // and delete them.
                    if (objectList.isTruncated()) {
                        objectList = s3.listNextBatchOfObjects(objectList);
                    } else {
                        break;
                    }
                }

                // Delete all object versions (required for versioned buckets).
                VersionListing versionList = s3.listVersions(new ListVersionsRequest().withBucketName(bucketName));
                while (true) {
                    List<DeleteObjectsRequest.KeyVersion> versionsToDelete = versionList.getVersionSummaries().stream()
                            .map(v -> new DeleteObjectsRequest.KeyVersion(v.getKey(), v.getVersionId()))
                            .collect(Collectors.toList());

                    logger.info("Deleting {} object versions", versionsToDelete.size());

                    s3.deleteObjects(new DeleteObjectsRequest(bucketName).withKeys(versionsToDelete));

                    if (versionList.isTruncated()) {
                        versionList = s3.listNextBatchOfVersions(versionList);
                    } else {
                        break;
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
