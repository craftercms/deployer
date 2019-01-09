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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.PutObjectRequest;

import static org.craftercms.deployer.utils.ConfigUtils.*;

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
public class S3SyncProcessor extends AbstractAwsDeploymentProcessor<AmazonS3ClientBuilder, AmazonS3> {

    private static final Logger logger = LoggerFactory.getLogger(S3SyncProcessor.class);

    public static final String CONFIG_KEY_URL = "url";

    public static final String DELIMITER = "/";

    /**
     * URL for the local git repository
     */
    protected String localRepoUrl;

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
        s3Url = new AmazonS3URI(StringUtils.appendIfMissing(getRequiredStringProperty(config, CONFIG_KEY_URL), DELIMITER));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChangeSet doExecute(final Deployment deployment, final ProcessorExecution execution,
                                  final ChangeSet filteredChangeSet) throws DeployerException {
        logger.info("Performing S3 sync...");
        logger.debug("Syncing with bucket {}", s3Url);

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
            logger.error("Error connecting to S3", e);
            throw new DeployerException("Error connecting to S3", e);
        }

        return null;
    }

    /**
     * Performs the upload of the given files.
     * @param client AWS S3 client
     * @param files list of files to upload
     * @throws DeployerException if there is any error reading or uploading the files
     */
    protected void uploadFiles(AmazonS3 client, List<String> files) throws DeployerException {
        logger.info("Uploading {} files", files.size());
        for(String file : files) {
            logger.debug("Uploading file: {}", file);
            try {
                File localFile = new File(localRepoUrl, file);
                PutObjectRequest request = new PutObjectRequest(s3Url.getBucket(), getS3Key(file), localFile);
                client.putObject(request);
            } catch (Exception e) {
                logger.error("Error uploading file " + file, e);
                throw new DeployerException("Error uploading file " + file, e);
            }
            logger.debug("Uploaded file: {}", file);
        }
        logger.debug("Uploads completed");
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
                logger.error("Error deleting files", e);
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
        return s3Url.getKey() + siteName + file;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected AmazonS3ClientBuilder createClientBuilder() {
        return AmazonS3ClientBuilder.standard();
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
    public void destroy() {
        // nothing to do...
    }

}
