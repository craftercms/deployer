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
package org.craftercms.deployer.utils.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;

import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Helper class the configures a {@code AwsClientBuilder} with properties like region and credentials.
 *
 * @author avasquez
 */
public class AwsClientBuilderConfigurer {

    public static final String CONFIG_KEY_REGION = "region";
    public static final String CONFIG_KEY_ACCESS_KEY = "accessKey";
    public static final String CONFIG_KEY_SECRET_KEY = "secretKey";

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

    /**
     * Main constructor Extracts the region and credentials from the config.
     *
     * @param config the config with the client properties
     * @throws ConfigurationException if an exception occurs while reading the configuration
     */
    public AwsClientBuilderConfigurer(Configuration config) throws ConfigurationException {
        if (config.containsKey(CONFIG_KEY_REGION)) {
            region = getStringProperty(config, CONFIG_KEY_REGION);
        }
        if (config.containsKey(CONFIG_KEY_ACCESS_KEY) && config.containsKey(CONFIG_KEY_SECRET_KEY)) {
            accessKey = getStringProperty(config, CONFIG_KEY_ACCESS_KEY);
            secretKey = getStringProperty(config, CONFIG_KEY_SECRET_KEY);
        }
    }

    /**
     * Configures the specified builder, with any credentials and other properties provided in the configuration.
     *
     * @param builder the AWS client builder
     */
    public void configureClientBuilder(AwsClientBuilder<?, ?> builder) {
        if (StringUtils.isNotEmpty(region)) {
            builder.withRegion(region);
        }
        if (StringUtils.isNotEmpty(accessKey) && StringUtils.isNotEmpty(secretKey)) {
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        }
    }

}
