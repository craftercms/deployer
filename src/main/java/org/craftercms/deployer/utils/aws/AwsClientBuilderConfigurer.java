/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.utils.aws;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;

import java.net.URI;

import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Helper class the configures a {@code AwsClientBuilder} with properties like region and credentials.
 *
 * @author avasquez
 */
public class AwsClientBuilderConfigurer<ClientBuilderSubclass extends AwsClientBuilder> {

    public static final String CONFIG_KEY_REGION = "region";
    public static final String CONFIG_KEY_ENDPOINT = "endpoint";
    public static final String CONFIG_KEY_ACCESS_KEY = "accessKey";
    public static final String CONFIG_KEY_SECRET_KEY = "secretKey";

    /**
     * AWS Region
     */
    protected String region;
    protected String endpoint;
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
        if (config.containsKey(CONFIG_KEY_ENDPOINT)) {
            endpoint = getStringProperty(config, CONFIG_KEY_ENDPOINT);
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
    public void configureClientBuilder(ClientBuilderSubclass builder) {
        if (StringUtils.isNotEmpty(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        } else if (StringUtils.isNotEmpty(region)) {
            builder.region(Region.of(region));
        }

        if (StringUtils.isNotEmpty(accessKey) && StringUtils.isNotEmpty(secretKey)) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        }
    }

}
