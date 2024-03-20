/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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
import com.amazonaws.services.s3.AmazonS3URI;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.lifecycle.AbstractLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.craftercms.deployer.utils.aws.AwsS3ClientBuilderConfigurer;

import java.beans.ConstructorProperties;

import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

public class DuplicateS3LifecycleHook extends AbstractLifecycleHook {

    protected static final String CONFIG_KEY_SOURCE_CONFIG = "sourceConfig";
    protected static final String CONFIG_KEY_URL = "url";
    protected static final String DELIMITER = "/";
    private AwsClientBuilderConfigurer<AmazonS3ClientBuilder> builderConfigurer;
    private AmazonS3URI s3Url;
    private AmazonS3URI srcS3Url;
    private final String siteName;
    private final String sourceSiteName;

    @ConstructorProperties({"siteName", "sourceSiteName"})
    public DuplicateS3LifecycleHook(String siteName, String sourceSiteName) {
        this.siteName = siteName;
        this.sourceSiteName = sourceSiteName;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException, DeployerException {
        builderConfigurer = new AwsS3ClientBuilderConfigurer(config);
        s3Url = new AmazonS3URI(appendIfMissing(getRequiredStringProperty(config, CONFIG_KEY_URL), DELIMITER));

        Configuration srcTargetConfig = config.subset(CONFIG_KEY_SOURCE_CONFIG);
        srcS3Url = new AmazonS3URI(appendIfMissing(getRequiredStringProperty(srcTargetConfig, CONFIG_KEY_URL), DELIMITER));
    }

    protected AmazonS3 buildClient(AwsClientBuilderConfigurer<AmazonS3ClientBuilder> builderConfigurer) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builderConfigurer.configureClientBuilder(builder);
        return builder.build();
    }

    @Override
    protected void doExecute(Target target) throws DeployerException {
        AmazonS3 client = buildClient(builderConfigurer);
        logger.info("Starting S3 content duplicate from '{}' for site '{}' to '{}' for site '{}'", srcS3Url, sourceSiteName, s3Url, siteName);

        // TODO: list all files in the repo and copy them from source to the target bucket
//        logger.info("Completed S3 content duplicate from '{}' for site '{}' to '{}' for site '{}'", srcS3Url, sourceSiteName, s3Url, siteName);
    }
}
