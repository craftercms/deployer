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
package org.craftercms.deployer.impl.processors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Post processor that writes the deployment result to an output file for later access, whenever a deployment fails or files where
 * processed.
 *
 * @author avasquez
 */
public class FileOutputProcessor extends AbstractPostDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileOutputProcessor.class);

    protected static final String OUTPUT_FILE_PARAM_NAME = "outputFile";
    protected static final String[] HEADERS = {
            "status", "duration", "start", "end", "created_files", "deleted_files", "updated_files"
    };

    protected File outputFolder;

    /**
     * Sets the output folder where the deployments results will be written to.
     */
    @Required
    public void setOutputFolder(File outputFolder) {
        this.outputFolder = outputFolder;
    }

    @Override
    public void doInit(Configuration config) throws DeployerException {
        if (!outputFolder.exists()) {
            try {
                FileUtils.forceMkdir(outputFolder);
            } catch (IOException e) {
                throw new DeployerException("Failed to create output folder " + outputFolder, e);
            }
        }
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected ChangeSet doPostProcess(Deployment deployment, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        File outputFile = getOutputFile(deployment);
        try (FileWriter fileWriter = new FileWriter(outputFile, true)) {
            CSVPrinter printer;
            if(outputFile.exists() && outputFile.length() > 0) {
                printer = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);
            } else {
                printer = new CSVPrinter(fileWriter, CSVFormat.DEFAULT.withHeader(HEADERS));
            }

            ChangeSet changeSet = deployment.getChangeSet();

            printer.printRecord(
                    deployment.getStatus(),
                    deployment.getDuration(),
                    deployment.getStart().toInstant(),
                    deployment.getEnd().toInstant(),
                    ListUtils.emptyIfNull(changeSet.getCreatedFiles()),
                    ListUtils.emptyIfNull(changeSet.getUpdatedFiles()),
                    ListUtils.emptyIfNull(changeSet.getDeletedFiles())
            );
        } catch (IOException e) {
            throw new DeployerException("Error while writing deployment output file " + outputFile, e);
        }

        deployment.addParam(OUTPUT_FILE_PARAM_NAME, outputFile);

        logger.info("Successfully wrote deployment output to {}", outputFile);

        return null;
    }

    protected File getOutputFile(Deployment deployment) {
        String targetId = deployment.getTarget().getId();
        String outputFilename = targetId + "-deployments.csv";

        return new File(outputFolder, outputFilename);
    }

}
