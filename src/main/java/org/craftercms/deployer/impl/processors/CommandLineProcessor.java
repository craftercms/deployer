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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import static org.craftercms.deployer.utils.ConfigUtils.*;

/**
 * Processor that runs a command line process.
 *
 * @author avasquez
 */
public class CommandLineProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineProcessor.class);

    protected static final String WORKING_DIR_CONFIG_KEY = "workingDir";
    protected static final String COMMAND_CONFIG_KEY = "command";
    protected static final String PROCESS_TIMEOUT_SECS_CONFIG_KEY = "processTimeoutSecs";

    protected static final long DEFAULT_PROCESS_TIMEOUT_SECS = 30;

    // Config properties (populated on init)

    private String workingDir;
    private String command;
    private long processTimeoutSecs;

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        workingDir = getStringProperty(config, WORKING_DIR_CONFIG_KEY);
        command = getRequiredStringProperty(config, COMMAND_CONFIG_KEY);
        processTimeoutSecs = getLongProperty(config, PROCESS_TIMEOUT_SECS_CONFIG_KEY, DEFAULT_PROCESS_TIMEOUT_SECS);
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s"));

        if (StringUtils.isNotEmpty(workingDir)) {
            processBuilder.directory(new File(workingDir));
        }

        processBuilder.redirectErrorStream(true);

        logger.info("Executing command: {}", command);

        try {
            Process process = processBuilder.start();
            process.waitFor(processTimeoutSecs, TimeUnit.SECONDS);

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String str;
                while ((str = reader.readLine()) != null) {
                    logger.info("PROCESS OUTPUT: {}", str);
                }
            }

            logger.info("Process finished with exit code {}", process.exitValue());
        } catch (IOException | InterruptedException e) {
            throw new DeployerException("Error while executing command", e);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

}
