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

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.transfer.MultipleFileUpload;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

import static org.craftercms.deployer.utils.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} that syncs files to an AWS S3 Bucket
 * Can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>url:</strong> AWS S3 bucket URL to upload files</li>
 * </ul>
 *
 * @author joseross
 */
public class S3SyncProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(S3SyncProcessor.class);

    protected static final String CONFIG_KEY_URL = "url";

    protected static final String DELIMITER = "/";

    /**
     * URL for the local git repository
     */
    protected String localRepoUrl;

    // Config properties (populated on init)

    /**
     * Helper class the configures credentials and other properties for a {@link AmazonS3} client.
     */
    protected AwsClientBuilderConfigurer builderConfigurer;
    /**
     * AWS S3 bucket URL
     */
    protected AmazonS3URI s3Url;

    @Required
    public void setLocalRepoUrl(final String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit(final Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        s3Url = new AmazonS3URI(StringUtils.appendIfMissing(getRequiredStringProperty(config, CONFIG_KEY_URL), DELIMITER));
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

        TransferManager transferManager = TransferManagerBuilder.standard().withS3Client(client).build();
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

    /**
     * Builds the AWS S3 key for the given file
     * @param file relative path of the file
     * @return the full S3 key
     */
    protected String getS3Key(String file) {
        String key = siteName + file;
        if(StringUtils.isNotEmpty(s3Url.getKey())) {
            return s3Url.getKey() + key;
        } else {
            return key;
        }
    }


    /**
     * Builds the {@link AmazonS3} client.
     */
    protected AmazonS3 buildClient() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builderConfigurer.configureClientBuilder(builder);

        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }
}
