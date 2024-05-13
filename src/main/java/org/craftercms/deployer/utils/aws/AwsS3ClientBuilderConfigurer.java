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
import org.craftercms.commons.config.ConfigurationException;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

import static org.craftercms.commons.config.ConfigUtils.getBooleanProperty;

/**
 * {@link AwsClientBuilderConfigurer} extension for S3 clients.
 */
public class AwsS3ClientBuilderConfigurer extends AwsClientBuilderConfigurer<S3ClientBuilder> {

    public static final String CONFIG_KEY_PATH_STYLE_ACCESS_ENABLED = "pathStyleAccess";

    /**
     * Whether to use path style access or not
     */
    protected boolean pathStyleAccessEnabled = false;

    /**
     * Main constructor Extracts the region and credentials from the config.
     *
     * @param config the config with the client properties
     * @throws ConfigurationException if an exception occurs while reading the configuration
     */
    public AwsS3ClientBuilderConfigurer(Configuration config) throws ConfigurationException {
        super(config);
        if (config.containsKey(CONFIG_KEY_PATH_STYLE_ACCESS_ENABLED)) {
            pathStyleAccessEnabled = getBooleanProperty(config, CONFIG_KEY_PATH_STYLE_ACCESS_ENABLED);
        }
    }

    @Override
    public void configureClientBuilder(S3ClientBuilder builder) {
        super.configureClientBuilder(builder);
        if (pathStyleAccessEnabled) {
            builder.serviceConfiguration(S3Configuration.builder()
                    .pathStyleAccessEnabled(true)
                    .build());
        }
    }
}
