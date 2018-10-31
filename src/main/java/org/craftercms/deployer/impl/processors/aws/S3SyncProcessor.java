package org.craftercms.deployer.impl.processors.aws;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.PutObjectRequest;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} that syncs files to an AWS S3 Bucket
 * @author joseross
 */
public class S3SyncProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(S3SyncProcessor.class);

    public static final String CONFIG_KEY_URL = "url";
    public static final String CONFIG_KEY_REGION = "region";
    public static final String CONFIG_KEY_ACCESS_KEY = "accessKey";
    public static final String CONFIG_KEY_SECRET_KEY = "secretKey";

    public static final String DELIMITER = "/";

    /**
     * URL for the local git repository
     */
    protected String localRepoUrl;

    /**
     * AWS S3 Bucket URL
     */
    protected AmazonS3URI s3Url;

    /**
     * AWS Region
     */
    protected String region;

    /**
     * AWS Access Key
     */
    protected String accessKey;

    /**
     * AWS Secret Key
     */
    protected String secretKey;

    @Required
    public void setLocalRepoUrl(final String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit(final Configuration config) throws DeployerException {
        s3Url = new AmazonS3URI(StringUtils.appendIfMissing(
            ConfigUtils.getRequiredStringProperty(config, CONFIG_KEY_URL), DELIMITER));
        if(config.containsKey(CONFIG_KEY_REGION)) {
            region = ConfigUtils.getStringProperty(config, CONFIG_KEY_REGION);
        }
        if(config.containsKey(CONFIG_KEY_ACCESS_KEY) && config.containsKey(CONFIG_KEY_SECRET_KEY)) {
            accessKey = ConfigUtils.getStringProperty(config, CONFIG_KEY_ACCESS_KEY);
            secretKey = ConfigUtils.getStringProperty(config, CONFIG_KEY_SECRET_KEY);
        }
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
            AmazonS3 client = getClient();

            List<String> changedFiles = ListUtils.union(filteredChangeSet.getCreatedFiles(), filteredChangeSet.getUpdatedFiles());

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
                DeleteObjectsRequest request = new DeleteObjectsRequest(s3Url.getBucket()).withKeys(keys.toArray(new String[] {}));
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
     * Builds the AWS S3 client
     * @return the client
     */
    protected AmazonS3 getClient() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        if(StringUtils.isNotEmpty(region)) {
            builder.withRegion(region);
        }
        if(StringUtils.isNotEmpty(accessKey) && StringUtils.isNotEmpty(secretKey)) {
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        }
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
    public void destroy() {
        // nothing to do...
    }

}
