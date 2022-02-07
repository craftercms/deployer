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
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ExecutorService;

import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.apache.commons.lang3.StringUtils.prependIfMissing;
import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Base implementation of {@link AbstractMainDeploymentProcessor} for processors that use AWS S3.
 * Can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>url:</strong> AWS S3 bucket URL to upload files</li>
 * </ul>
 *
 * @author joseross
 * @since 3.1.8
 */
public abstract class AbstractS3Processor extends AbstractMainDeploymentProcessor {

    protected static final String CONFIG_KEY_URL = "url";
    protected static final String MACRO_SITENAME = "{siteName}";

    protected static final String DELIMITER = "/";

    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Helper class the configures credentials and other properties for a {@link AmazonS3} client.
     */
    protected AwsClientBuilderConfigurer builderConfigurer;

    /**
     * AWS S3 bucket URL
     */
    protected AmazonS3URI s3Url;

    /**
     * Thread pool to use for {@link TransferManager} instances
     */
    protected ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public AbstractS3Processor(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit(final Configuration config) throws ConfigurationException {
        builderConfigurer = new AwsClientBuilderConfigurer(config);
        s3Url = new AmazonS3URI(appendIfMissing(getRequiredStringProperty(config, CONFIG_KEY_URL), DELIMITER));

        // use true as default for backward compatibility
        failDeploymentOnFailure = config.getBoolean(FAIL_DEPLOYMENT_CONFIG_KEY, true);
    }

    /**
     * Returns the base key from the S3 URL, making sure to replace the {@code {siteName}} macro instances
     */
    protected String getS3BaseKey() {
        String baseKey = s3Url.getKey();
        if (StringUtils.isNotEmpty(baseKey)) {
            return baseKey.replace(MACRO_SITENAME, siteName);
        } else {
            return StringUtils.EMPTY;
        }
    }

    /**
     * Builds the AWS S3 key for the given file
     * @param file relative path of the file
     * @return the full S3 key
     */
    protected String getS3Key(String file) {
        return StringUtils.appendIfMissing(getS3BaseKey(), DELIMITER) + StringUtils.stripStart(file, DELIMITER);
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
     * Builds the {@link TransferManager} using the shared {@link ExecutorService}
     */
    protected TransferManager buildTransferManager(AmazonS3 client) {
        return TransferManagerBuilder
                .standard()
                .withS3Client(client)
                .withExecutorFactory(() -> threadPoolTaskExecutor.getThreadPoolExecutor())
                .withShutDownThreadPools(false)
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

}
