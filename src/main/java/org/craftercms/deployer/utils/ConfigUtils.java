/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.YamlConfiguration;
import org.craftercms.deployer.api.exceptions.DeployerConfigurationException;
import org.craftercms.deployer.api.exceptions.MissingConfigurationPropertyException;
import org.springframework.core.io.Resource;

/**
 * Utility methods for handling YAML/Apache Commons Configuration.
 *
 * @author avasquez
 */
public class ConfigUtils {

    private ConfigUtils() {
    }

    /**
     * Loads the specified file as {@link YamlConfiguration}.
     *
     * @param file the YAML configuration file to load
     * @return the YAML configuration
     * @throws DeployerConfigurationException if an error occurred
     */
    public static YamlConfiguration loadYamlConfiguration(File file) throws DeployerConfigurationException {
        try {
            try (Reader reader = new BufferedReader(new FileReader(file))) {
                return doLoadYamlConfiguration(reader);
            }
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to load YAML configuration at " + file, e);
        }
    }

    /**
     * Loads the specified resource as {@link YamlConfiguration}.
     *
     * @param resource the YAML configuration resource to load
     * @return the YAML configuration
     * @throws DeployerConfigurationException if an error occurred
     */
    public static YamlConfiguration loadYamlConfiguration(Resource resource) throws DeployerConfigurationException {
        try {
            try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
                return doLoadYamlConfiguration(reader);
            }
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to load YAML configuration at " + resource, e);
        }
    }

    /**
     * Returns the specified String property from the configuration
     *
     * @param config the configuration
     * @param key    the key of the property
     * @return the String value of the property, or null if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static String getStringProperty(Configuration config, String key) throws DeployerConfigurationException {
        return getStringProperty(config, key, null);
    }

    /**
     * Returns the specified String property from the configuration
     *
     * @param config       the configuration
     * @param key          the key of the property
     * @param defaultValue the default value if the property is not found
     * @return the String value of the property, or the default value if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static String getStringProperty(Configuration config, String key,
                                           String defaultValue) throws DeployerConfigurationException {
        try {
            return config.getString(key, defaultValue);
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    /**
     * Returns the specified String property from the configuration. If the property is missing a
     * {@link MissingConfigurationPropertyException} is thrown.
     *
     * @param config the configuration
     * @param key    the key of the property
     * @return the String value of the property
     * @throws MissingConfigurationPropertyException if the property is missing from the configuration
     * @throws DeployerConfigurationException        if an error occurred
     */
    public static String getRequiredStringProperty(Configuration config,
                                                   String key) throws DeployerConfigurationException {
        String property = getStringProperty(config, key);
        if (StringUtils.isEmpty(property)) {
            throw new MissingConfigurationPropertyException(key);
        } else {
            return property;
        }
    }

    /**
     * Returns the specified Boolean property from the configuration
     *
     * @param config the configuration
     * @param key    the key of the property
     * @return the Boolean value of the property, or null if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static Boolean getBooleanProperty(Configuration config, String key) throws DeployerConfigurationException {
        return getBooleanProperty(config, key, null);
    }

    /**
     * Returns the specified Boolean property from the configuration
     *
     * @param config       the configuration
     * @param key          the key of the property
     * @param defaultValue the default value if the property is not found
     * @return the Boolean value of the property, or the default value if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static Boolean getBooleanProperty(Configuration config, String key,
                                             Boolean defaultValue) throws DeployerConfigurationException {
        try {
            return config.getBoolean(key, defaultValue);
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    /**
     * Returns the specified Integer property from the configuration
     *
     * @param config the configuration
     * @param key    the key of the property
     * @return the Integer value of the property, or null if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static Integer getIntegerProperty(Configuration config, String key) throws DeployerConfigurationException {
        return getIntegerProperty(config, key, null);
    }

    /**
     * Returns the specified Integer property from the configuration
     *
     * @param config       the configuration
     * @param key          the key of the property
     * @param defaultValue the default value if the property is not found
     * @return the Integer value of the property, or the default value if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static Integer getIntegerProperty(Configuration config, String key,
                                             Integer defaultValue) throws DeployerConfigurationException {
        try {
            return config.getInteger(key, defaultValue);
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    /**
     * Returns the specified Long property from the configuration
     *
     * @param config the configuration
     * @param key    the key of the property
     * @return the Long value of the property, or null if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static Long getLongProperty(Configuration config, String key) throws DeployerConfigurationException {
        return getLongProperty(config, key, null);
    }

    /**
     * Returns the specified Long property from the configuration
     *
     * @param config       the configuration
     * @param key          the key of the property
     * @param defaultValue the default value if the property is not found
     * @return the Long value of the property, or the default value if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static Long getLongProperty(Configuration config, String key,
                                       Long defaultValue) throws DeployerConfigurationException {
        try {
            return config.getLong(key, defaultValue);
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    /**
     * Returns the specified String array property from the configuration. A String array property is normally
     * specified as
     * a String with values separated by commas in the configuration.
     *
     * @param config the configuration
     * @param key    the key of the property
     * @return the String array value of the property, or null if not found
     * @throws DeployerConfigurationException if an error occurred
     */
    public static String[] getStringArrayProperty(Configuration config,
                                                  String key) throws DeployerConfigurationException {
        try {
            return config.getStringArray(key);
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to retrieve property '" + key + "'", e);
        }
    }

    /**
     * Returns the specified String array property from the configuration. If the property is missing a
     * {@link MissingConfigurationPropertyException} is thrown. A String array property is normally specified as
     * a String with values separated by commas in the configuration.
     *
     * @param config the configuration
     * @param key    the key of the property
     * @return the String value of the property
     * @throws MissingConfigurationPropertyException if the property is missing from the configuration
     * @throws DeployerConfigurationException        if an error occurred
     */
    public static String[] getRequiredStringArrayProperty(Configuration config,
                                                          String key) throws DeployerConfigurationException {
        String[] property = getStringArrayProperty(config, key);
        if (ArrayUtils.isEmpty(property)) {
            throw new MissingConfigurationPropertyException(key);
        } else {
            return property;
        }
    }

    /**
     * Returns the sub-configuration tree whose root is the specified key.
     *
     * @param config the configuration
     * @param key    the key of the configuration tree
     * @return the sub-configuration tree, or null if not found
     * @throws DeployerConfigurationException if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<HierarchicalConfiguration> getConfigurationsAt(HierarchicalConfiguration config,
                                                                      String key) throws
        DeployerConfigurationException {
        try {
            return config.configurationsAt(key);
        } catch (Exception e) {
            throw new DeployerConfigurationException("Failed to retrieve sub-configurations at '" + key + "'", e);
        }
    }

    /**
     * Returns the sub-configuration tree whose root is the specified key. If the tree is missing a
     * {@link MissingConfigurationPropertyException} is thrown
     *
     * @param config the configuration
     * @param key    the key of the configuration tree
     * @return the sub-configuration tree, or null if not found
     * @throws MissingConfigurationPropertyException if the property is missing from the configuration
     * @throws DeployerConfigurationException        if an error occurs
     */
    @SuppressWarnings("unchecked")
    public static List<HierarchicalConfiguration> getRequiredConfigurationsAt(HierarchicalConfiguration config,
                                                                              String key) throws
        DeployerConfigurationException {
        List<HierarchicalConfiguration> configs = getConfigurationsAt(config, key);
        if (CollectionUtils.isEmpty(configs)) {
            throw new MissingConfigurationPropertyException(key);
        } else {
            return configs;
        }
    }

    protected static YamlConfiguration doLoadYamlConfiguration(
        Reader reader) throws IOException, ConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.read(reader);

        return configuration;
    }

}
