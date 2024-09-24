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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.impl.lifecycle.AbstractLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.craftercms.deployer.utils.aws.AwsS3Utils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.paginators.ListObjectVersionsIterable;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import java.util.ArrayList;
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
            S3Client s3 = buildClient();

            if (!bucketExist(s3, bucketName)) {
                return;
            }

            logger.info("Emptying bucket '{}'...", bucketName);

            // Delete all objects from the bucket. This is sufficient
            // for unversioned buckets. For versioned buckets, when you attempt to delete objects, Amazon S3 inserts
            // delete markers for all objects, but doesn't delete the object versions.
            // To delete objects from versioned buckets, delete all the object versions before deleting
            // the bucket (see below for an example).
            deleteAllObjects(s3);

            // Delete all object versions and delete marker (required for versioned buckets).
            deleteAllVersionsAndMarkers(s3);
        } catch (Exception e) {
            throw new DeployerException("Error while trying to clear S3 bucket '" + bucketName + "'", e);
        }
    }

    protected S3Client buildClient() {
        S3ClientBuilder builder = S3Client.builder();
        builderConfigurer.configureClientBuilder(builder);

        return builder.build();
    }

    /**
     * Check if a bucket exist
     * @param s3Client an instance of {@link S3Client}
     * @param bucketName bucket name
     * @return true if bucket exist, false otherwise
     */
    private boolean bucketExist(S3Client s3Client, String bucketName) {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            HeadBucketResponse response = s3Client.headBucket(request);
            return response.sdkHttpResponse().isSuccessful();
        } catch (NoSuchBucketException e) {
            logger.debug("Error while get head of bucket '{}", bucketName, e);
            return false;
        }
    }

    /**
     * Delete all S3 objects
     * @param s3 the s3 client
     */
    private void deleteAllObjects(S3Client s3) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();
        ListObjectsV2Iterable listObjectsV2Responses = s3.listObjectsV2Paginator(request);
        for (ListObjectsV2Response objectList: listObjectsV2Responses) {
            List<ObjectIdentifier> objectsToDelete = objectList.contents().stream().map(s ->
                    ObjectIdentifier.builder()
                            .key(s.key())
                            .build()
            ).collect(Collectors.toList());

            if (CollectionUtils.isNotEmpty(objectsToDelete)) {
                logger.info("Deleting {} objects", objectsToDelete.size());
                s3.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder()
                                .objects(objectsToDelete)
                                .build())
                        .build());
            } else {
                logger.info("No objects to delete");
            }
        }
    }

    /**
     * Delete all versions and delete markers
     * @param s3 the s3 client
     */
    private void deleteAllVersionsAndMarkers(S3Client s3) {
        ListObjectVersionsRequest listObjectVersionsRequest = ListObjectVersionsRequest.builder()
                .bucket(bucketName)
                .build();
        ListObjectVersionsIterable listObjectVersionsResponses = s3.listObjectVersionsPaginator(listObjectVersionsRequest);
        for (ListObjectVersionsResponse versionList: listObjectVersionsResponses) {
            List<ObjectIdentifier> versionsToDelete = new ArrayList<>();
            versionList.versions().forEach(v -> versionsToDelete.add(
                    ObjectIdentifier.builder()
                            .key(v.key())
                            .versionId(v.versionId())
                            .build()
            ));

            versionList.deleteMarkers().forEach(m -> versionsToDelete.add(
                    ObjectIdentifier.builder()
                            .key(m.key())
                            .versionId(m.versionId())
                            .build()
            ));

            if (CollectionUtils.isNotEmpty(versionsToDelete)) {
                logger.info("Deleting {} object versions and delete markers", versionsToDelete.size());
                for (List<ObjectIdentifier> subList : ListUtils.partition(versionsToDelete, AwsS3Utils.MAX_DELETE_KEYS_PER_REQUEST)) {
                    DeleteObjectsResponse result = s3.deleteObjects(DeleteObjectsRequest.builder()
                            .bucket(bucketName)
                            .delete(Delete.builder().objects(subList).build())
                            .build());
                    logger.debug("Deleted object versions and delete markers: {}", result.deleted());
                }
            } else {
                logger.info("No object versions to delete");
            }
        }
    }

}
