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
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 1/2/17.
 */
public class FileOutputProcessor extends AbstractPostDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileOutputProcessor.class);

    public static final String OUTPUT_FILE_ATTRIBUTE_NAME = "outputFile";

    protected File outputFolder;
    protected String timestampPattern;
    protected DateTimeFormatter timestampFormatter;
    protected ObjectMapper objectMapper;

    @Required
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    @Required
    public void setTimestampPattern(String timestampPattern) {
        this.timestampPattern = timestampPattern;
    }

    @Required
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void init(Configuration config) throws DeployerException {
        timestampFormatter = DateTimeFormatter.ofPattern(timestampPattern);

        if (!outputFolder.exists()) {
            try {
                FileUtils.forceMkdir(outputFolder);
            } catch (IOException e) {
                throw new DeployerException("Failed to create output folder " + outputFolder, e);
            }
        }
    }

    @Override
    public void destroy() throws DeployerException {
    }

    @Override
    protected void doExecute(Deployment deployment, Map<String, Object> params) throws DeployerException {
        File outputFile = getOutputFile(deployment);

        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, deployment);
        } catch (IOException e) {
            throw new DeployerException("Error while writing deployment output file " + outputFile, e);
        }

        deployment.addAttribute(OUTPUT_FILE_ATTRIBUTE_NAME, outputFile);

        logger.info("Successfully wrote deployment output to {}", outputFile);
    }

    protected File getOutputFile(Deployment deployment) {
        String targetId = deployment.getTarget().getId();
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
