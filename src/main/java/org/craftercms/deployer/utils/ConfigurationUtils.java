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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.YamlConfiguration;
import org.craftercms.deployer.api.exceptions.DeploymentConfigurationException;
import org.craftercms.deployer.api.exceptions.MissingConfigurationPropertyException;
import org.springframework.core.io.Resource;

/**
 * Created by alfonsovasquez on 12/22/16.
 */
public class ConfigurationUtils {

    private ConfigurationUtils() {
    }

    public static YamlConfiguration loadYamlConfiguration(File file) throws DeploymentConfigurationException {
        try {
            try (Reader reader = new BufferedReader(new FileReader(file))) {
                return doLoadYamlConfiguration(reader);
            }
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to load YAML configuration at " + file, e);
        }
    }

    public static YamlConfiguration loadYamlConfiguration(Resource resource) throws DeploymentConfigurationException {
        try {
            try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
                return doLoadYamlConfiguration(reader);
            }
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to load YAML configuration at " + resource, e);
        }
    }

    public static String getString(Configuration config, String key) throws DeploymentConfigurationException {
        try {
            return config.getString(key);
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    public static String getString(Configuration config, String key, String defaultValue) throws DeploymentConfigurationException {
        try {
            return config.getString(key, defaultValue);
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    public static String getRequiredString(Configuration config, String key) throws DeploymentConfigurationException {
        String property = getString(config, key);
        if (StringUtils.isEmpty(property)) {
            throw new MissingConfigurationPropertyException("Missing required property '" + key + "'");
        } else {
            return property;
        }
    }

    public static boolean getBoolean(Configuration config, String key) throws DeploymentConfigurationException {
        try {
            return config.getBoolean(key);
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    public static boolean getBoolean(Configuration config, String key, boolean defaultValue) throws DeploymentConfigurationException {
        try {
            return config.getBoolean(key, defaultValue);
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<HierarchicalConfiguration> getConfigurationsAt(HierarchicalConfiguration config,
                                                                      String key) throws DeploymentConfigurationException {
        try {
            return config.configurationsAt(key);
        } catch (Exception e) {
            throw new DeploymentConfigurationException("Failed to retrieve sub-configurations at '" + key + "'", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<HierarchicalConfiguration> getRequiredConfigurationsAt(HierarchicalConfiguration config,
                                                                              String key) throws DeploymentConfigurationException {
        List<HierarchicalConfiguration> configs = getConfigurationsAt(config, key);
        if (CollectionUtils.isEmpty(configs)) {
            throw new MissingConfigurationPropertyException("Missing required sub-configurations at '" + key + "'");
        } else {
            return configs;
        }
    }

    protected static YamlConfiguration doLoadYamlConfiguration(Reader reader) throws IOException, ConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.read(reader);

        return configuration;
    }

}
