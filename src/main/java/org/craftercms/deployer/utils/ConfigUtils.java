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
package org.craftercms.deployer.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.config.YamlConfiguration;
import org.springframework.core.io.Resource;

/**
 * Utility methods for handling YAML/Apache Commons Configuration.
 *
 * @author avasquez
 */
public class ConfigUtils extends org.craftercms.commons.config.ConfigUtils {

    /**
     * Loads the specified file as {@link YamlConfiguration}.
     *
     * @param file the YAML configuration file to load
     * @return the YAML configuration
     * @throws ConfigurationException if an error occurred
     */
    public static YamlConfiguration readYamlConfiguration(File file) throws ConfigurationException {
        try {
            try (Reader reader = new BufferedReader(new FileReader(file))) {
                return doReadYamlConfiguration(reader);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load YAML configuration at " + file, e);
        }
    }

    /**
     * Loads the specified resource as {@link YamlConfiguration}.
     *
     * @param resource the YAML configuration resource to load
     * @return the YAML configuration
     * @throws ConfigurationException if an error occurred
     */
    public static YamlConfiguration readYamlConfiguration(Resource resource) throws ConfigurationException {
        try {
            try (Reader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), "UTF-8"))) {
                return doReadYamlConfiguration(reader);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Failed to load YAML configuration at " + resource, e);
        }
    }

    protected static YamlConfiguration doReadYamlConfiguration(Reader reader)
            throws IOException, org.apache.commons.configuration2.ex.ConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.read(reader);

        return configuration;
    }

    protected ConfigUtils() {
    }

}
