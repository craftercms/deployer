/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.deployer.utils;

import java.io.File;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.YamlConfiguration;
import org.craftercms.deployer.api.exceptions.DeploymentConfigurationException;
import org.craftercms.deployer.api.exceptions.MissingConfigurationPropertyException;

/**
 * Created by alfonsovasquez on 12/22/16.
 */
public class ConfigurationUtils {

    public static final String DEFAULT_CONFIG_FILE_ENCODING = "UTF-8";

    private ConfigurationUtils() {
    }

    public static YamlConfiguration loadYamlConfiguration(File yamlFile) throws DeploymentConfigurationException {
        try {
            Parameters params = new Parameters();
            FileBasedConfigurationBuilder<YamlConfiguration> builder = new FileBasedConfigurationBuilder<>(YamlConfiguration.class);

            builder.configure(params.hierarchical().setEncoding(DEFAULT_CONFIG_FILE_ENCODING).setFile(yamlFile));

            return builder.getConfiguration();
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to load YAML configuration at " + yamlFile, e);
        }
    }

    public static String getString(Configuration config, String key, boolean required) throws DeploymentConfigurationException {
        String property;
        try {
             property = config.getString(key);
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to retrieve property '" + key + "'", e);
        }

        if (StringUtils.isEmpty(property) && required) {
            throw new MissingConfigurationPropertyException("Missing required property '" + property + "'");
        } else {
            return property;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<HierarchicalConfiguration> configurationsAt(HierarchicalConfiguration config,
                                                                   String key, boolean required) throws DeploymentConfigurationException {
        List<HierarchicalConfiguration> configs;
        try {
            configs = config.configurationsAt(key);
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to retrieve sub-configurations at '" + key + "'", e);
        }

        if (CollectionUtils.isEmpty(configs) && required) {
            throw new MissingConfigurationPropertyException("Missing required sub-configurations at '" + key + "'");
        } else {
            return configs;
        }
    }

}
