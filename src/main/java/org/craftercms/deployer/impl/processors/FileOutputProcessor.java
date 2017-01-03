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
package org.craftercms.deployer.impl.processors;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 1/2/17.
 */
public class FileOutputProcessor extends AbstractPostDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileOutputProcessor.class);

    public static final String OUTPUT_FOLDER_PATH_CONFIG_KEY = "outputFolderPath";
    public static final String TIMESTAMP_PATTERN_CONFIG_KEY = "timestampPattern";

    public static final String OUTPUT_FILE_ATTRIBUTE_NAME = "outputFile";

    protected File outputFolder;
    protected String defaultOutputFolderPath;
    protected String defaultTimestampPattern;
    protected DateTimeFormatter timestampFormatter;
    protected ObjectMapper objectMapper;

    @Required
    public void setDefaultOutputFolderPath(String defaultOutputFolderPath) {
        this.defaultOutputFolderPath = defaultOutputFolderPath;
    }

    @Required
    public void setDefaultTimestampPattern(String defaultTimestampPattern) {
        this.defaultTimestampPattern = defaultTimestampPattern;
    }

    @Required
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doInit(Configuration mainConfig, Configuration processorConfig) throws DeploymentException {
        String outputFolderPath = ConfigurationUtils.getString(processorConfig, OUTPUT_FOLDER_PATH_CONFIG_KEY, defaultOutputFolderPath);
        String timestampPattern = ConfigurationUtils.getString(processorConfig, TIMESTAMP_PATTERN_CONFIG_KEY, defaultTimestampPattern);

        outputFolder = new File(outputFolderPath);
        timestampFormatter = DateTimeFormatter.ofPattern(timestampPattern);

        if (!outputFolder.exists()) {
            try {
                FileUtils.forceMkdir(outputFolder);
            } catch (IOException e) {
                throw new DeploymentException("Failed to create output folder " + outputFolder, e);
            }
        }
    }

    @Override
    protected void doExecute(Deployment deployment) throws DeploymentException {
        File outputFile = getOutputFile(deployment);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, deployment);
        } catch (IOException e) {
            throw new DeploymentException("Error while writing deployment output file " + outputFile, e);
        }

        deployment.addAttribute(OUTPUT_FILE_ATTRIBUTE_NAME, outputFile);

        logger.info("Successfully wrote deployment output to {}", outputFile);
    }

    protected File getOutputFile(Deployment deployment) {
        String targetId = deployment.getTargetContext().getId();
        ZonedDateTime start = deployment.getStart();
        String filenameTimestamp = start.format(timestampFormatter);
        String outputFilename = targetId + "-deployment-" + filenameTimestamp;
        int count = 0;
        File outputFile;

        do {
            outputFile = new File(outputFolder, outputFilename + (count != 0? count : "") + ".json");
            count++;
        } while (outputFile.exists());

        return outputFile;
    }

}
