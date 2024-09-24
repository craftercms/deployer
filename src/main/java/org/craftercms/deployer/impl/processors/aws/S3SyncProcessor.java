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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.aws.AwsS3Utils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.CompletedFileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.beans.ConstructorProperties;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.craftercms.commons.config.ConfigUtils.getBooleanProperty;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} that syncs files to an AWS S3 Bucket
 *
 * @author joseross
 * @since 3.1.0
 */
public class S3SyncProcessor extends AbstractS3Processor {

    public static final String CONFIG_KEY_IGNORE_BLOBS = "ignoreBlobs";

    /**
     * URL for the local git repository
     */
    protected String localRepoUrl;

    /**
     * The extension used for blob files in the repository
     */
    protected String blobExtension;

    /**
     * Indicates if blob files should not be uploaded to S3
     */
    protected boolean ignoreBlobs;

    @ConstructorProperties({"threadPoolTaskExecutor", "localRepoUrl", "blobExtension"})
    public S3SyncProcessor(ThreadPoolTaskExecutor threadPoolTaskExecutor, String localRepoUrl, String blobExtension) {
        super(threadPoolTaskExecutor);
        this.localRepoUrl = localRepoUrl;
        this.blobExtension = blobExtension;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        super.doInit(config);

        ignoreBlobs = getBooleanProperty(config, CONFIG_KEY_IGNORE_BLOBS, true);
    }

    @Override
    protected boolean shouldIncludeFile(String file) {
        return super.shouldIncludeFile(file) && !(ignoreBlobs && endsWith(file, blobExtension));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        logger.info("Performing S3 sync with bucket {}...", s3Url);

        try {
            S3AsyncClient asyncClient = buildAsyncClient();

            List<String> changedFiles =
                ListUtils.union(filteredChangeSet.getCreatedFiles(), filteredChangeSet.getUpdatedFiles());

            if (CollectionUtils.isNotEmpty(changedFiles)) {
                uploadFiles(asyncClient, changedFiles);
            }


            S3Client client = buildClient();
            if (CollectionUtils.isNotEmpty(filteredChangeSet.getDeletedFiles())) {
                deleteFiles(client, filteredChangeSet.getDeletedFiles());
            }
        } catch (S3Exception e) {
            throw new DeployerException("Error connecting to S3", e);
        }

        return null;
    }

    /**
     * Performs the upload of the given files.
     * @param client AWS S3 async client
     * @param paths list of files to upload
     * @throws DeployerException if there is any error reading or uploading the files
     */
    protected void uploadFiles(S3AsyncClient client, List<String> paths) throws DeployerException {
        logger.info("Uploading {} files", paths.size());

        S3TransferManager transferManager = buildTransferManager(client);

        try {
            List<CompletableFuture<CompletedFileUpload>> futures = paths.stream().map(path -> {
                UploadFileRequest uploadFileRequest = UploadFileRequest.builder()
                        .putObjectRequest(p -> p.bucket(getBucket()).key(getS3Key(path)))
                        .source(Paths.get(localRepoUrl, path))
                        .build();
                return transferManager.uploadFile(uploadFileRequest).completionFuture();
            }).collect(Collectors.toList());

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            logger.debug("Uploads completed");
        } catch (Exception e) {
            throw new DeployerException(format("Error uploading files '%s'", paths), e);
        } finally {
            transferManager.close();
        }
    }

    /**
     * Performs the delete of the given files.
     * @param client AWS S3 client
     * @param files list of files to delete
     * @throws DeployerException if there is any error deleting the files
     */
    protected void deleteFiles(S3Client client, List<String> files) throws DeployerException {
        if(CollectionUtils.isNotEmpty(files)) {
            logger.info("Deleting {} files", files.size());
            logger.debug("Deleting files: {}", files);
            List<String> keys =
                files.stream().map(this::getS3Key).collect(Collectors.toList());

            try {
                for (List<String> subList : ListUtils.partition(keys, AwsS3Utils.MAX_DELETE_KEYS_PER_REQUEST)) {
                    List<ObjectIdentifier> identifiers = subList.stream().map(s ->
                            ObjectIdentifier.builder()
                                    .key(s)
                                    .build())
                            .collect(Collectors.toList());

                    DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                            .bucket(getBucket())
                            .delete(Delete.builder().objects(identifiers).build())
                            .build();
                    DeleteObjectsResponse result = client.deleteObjects(request);

                    logger.debug("Deleted files: {}", result.deleted());
                }
            } catch (Exception e) {
                throw new DeployerException("Error deleting files", e);
            }
        }
    }

}
