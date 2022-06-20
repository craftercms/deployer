/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

    protected static final String[] HEADERS = {
            "mode", "status", "duration", "start", "end", "created_files", "updated_files", "deleted_files"
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
    protected void doDestroy() {
        // Do nothing
    }

    @Override
    public boolean supportsMode(Deployment.Mode mode) {
        // the output file should be always generated
        return true;
    }

    @Override
    protected ChangeSet doPostProcess(Deployment deployment, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        File outputFile = getOutputFile(deployment);
        try (FileWriter fileWriter = new FileWriter(outputFile, true)) {
            // Use a file printer to append to the full history in the FS
            CSVPrinter filePrinter;
            if(outputFile.exists() && outputFile.length() > 0) {
                filePrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT);
            } else {
                filePrinter = new CSVPrinter(fileWriter, CSVFormat.Builder.create().setHeader(HEADERS).build());
            }
            appendDeployment(filePrinter, deployment);
        } catch (IOException e) {
            throw new DeployerException("Error while writing deployment output file " + outputFile, e);
        }

        logger.info("Successfully wrote deployment output to {}", outputFile);

        return null;
    }

    protected void appendDeployment(CSVPrinter printer, Deployment deployment) throws IOException {
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
    }

    protected File getOutputFile(Deployment deployment) {
        String targetId = deployment.getTarget().getId();
        String outputFilename = targetId + "-deployments.csv";

        return new File(outputFolder, outputFilename);
    }

}
