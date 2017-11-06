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

import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Post processor that writes the deployment result to an output file for later access, whenever a deployment fails or files where
 * processed.
 *
 * @author avasquez
 */
public class FileOutputProcessor extends AbstractPostDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileOutputProcessor.class);

    public static final String OUTPUT_FILE_PARAM_NAME = "outputFile";

    protected File outputFolder;
    protected String timestampPattern;
    protected DateTimeFormatter timestampFormatter;
    protected CsvMapper objectMapper;

    /**
     * Sets the output folder where the deployments results will be written to.
     */
    @Required
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    /**
     * Sets the timestamp pattern to use on the output file names.
     */
    @Required
    public void setTimestampPattern(String timestampPattern) {
        this.timestampPattern = timestampPattern;
    }

    /**
     * Sets the CSV serializer to use to generate the output.
     */
    @Required
    public void setObjectMapper(CsvMapper objectMapper) {
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
    protected void doExecute(Deployment deployment) throws DeployerException {
        File outputFile = getOutputFile(deployment);
        CsvSchema formatSchema = objectMapper.schemaFor(Deployment.class);
        boolean useHeaders = !Files.exists(outputFile.toPath());
        try (FileWriter fileWriter = new FileWriter(outputFile, true)) {
            if(useHeaders) {
                formatSchema = formatSchema.withHeader();
            }
            objectMapper.writer(formatSchema).writeValue(fileWriter, deployment);
        } catch (IOException e) {
            throw new DeployerException("Error while writing deployment output file " + outputFile, e);
        }

        deployment.addParam(OUTPUT_FILE_PARAM_NAME, outputFile);

        logger.info("Successfully wrote deployment output to {}", outputFile);
    }

    protected File getOutputFile(Deployment deployment) {
        String targetId = deployment.getTarget().getId();
        ZonedDateTime now = ZonedDateTime.now();
        String filenameTimestamp = now.format(timestampFormatter);
        String outputFilename = targetId + "-deployments-" + filenameTimestamp;
        File outputFile = new File(outputFolder, outputFilename + ".csv");

        return outputFile;
    }

}
