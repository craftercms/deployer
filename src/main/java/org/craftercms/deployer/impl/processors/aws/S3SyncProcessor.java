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

package org.craftercms.deployer.impl.processors.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

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

    public S3SyncProcessor(ThreadPoolTaskExecutor threadPoolTaskExecutor, String localRepoUrl, String blobExtension) {
        super(threadPoolTaskExecutor);
        this.localRepoUrl = localRepoUrl;
        this.blobExtension = blobExtension;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        super.doInit(config);

        ignoreBlobs = getBooleanProperty(config, CONFIG_KEY_IGNORE_BLOBS, false);
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
            AmazonS3 client = buildClient();

            List<String> changedFiles =
                ListUtils.union(filteredChangeSet.getCreatedFiles(), filteredChangeSet.getUpdatedFiles());

            if (CollectionUtils.isNotEmpty(changedFiles)) {
                uploadFiles(client, changedFiles);
            }

            if (CollectionUtils.isNotEmpty(filteredChangeSet.getDeletedFiles())) {
                deleteFiles(client, filteredChangeSet.getDeletedFiles());
            }
        } catch (AmazonS3Exception e) {
            throw new DeployerException("Error connecting to S3", e);
        }

        return null;
    }

    /**
     * Performs the upload of the given files.
     * @param client AWS S3 client
     * @param paths list of files to upload
     * @throws DeployerException if there is any error reading or uploading the files
     */
    protected void uploadFiles(AmazonS3 client, List<String> paths) throws DeployerException {
        logger.info("Uploading {} files", paths.size());

        TransferManager transferManager = buildTransferManager(client);
        List<File> files = paths.stream().map(path -> new File(localRepoUrl, path)).collect(Collectors.toList());

        try {
            MultipleFileUpload upload = transferManager.uploadFileList(
                    s3Url.getBucket(), StringUtils.prependIfMissing(siteName, s3Url.getKey()),
                    new File(localRepoUrl), files);
            upload.waitForCompletion();

            logger.debug("Uploads completed");
        } catch (Exception e) {
            throw new DeployerException("Error uploading files " + paths, e);
        } finally {
            transferManager.shutdownNow(false);
        }
    }

    /**
     * Performs the delete of the given files.
     * @param client AWS S3 client
     * @param files list of files to delete
     * @throws DeployerException if there is any error deleting the files
     */
    protected void deleteFiles(AmazonS3 client, List<String> files) throws DeployerException {
        if(CollectionUtils.isNotEmpty(files)) {
            logger.info("Deleting {} files", files.size());
            logger.debug("Deleting files: {}", files);
            List<String> keys =
                files.stream().map(this::getS3Key).collect(Collectors.toList());

            try {
                DeleteObjectsRequest request =
                    new DeleteObjectsRequest(s3Url.getBucket()).withKeys(keys.toArray(new String[] {}));
                DeleteObjectsResult result = client.deleteObjects(request);

                logger.debug("Deleted files: {}", result.getDeletedObjects());
            } catch (Exception e) {
                throw new DeployerException("Error deleting files", e);
            }
        }
    }

}
