/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All rights reserved.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor that runs a command line process.
 *
 * @author avasquez
 */
public class CommandLineProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineProcessor.class);

    public static final String WORKING_DIR_CONFIG_KEY = "workingDir";
    public static final String COMMAND_CONFIG_KEY = "command";
    public static final String PROCESS_TIMEOUT_SECS_CONFIG_KEY = "processTimeoutSecs";

    public static final long DEFAULT_PROCESS_TIMEOUT_SECS = 30;

    private String workingDir;
    private String command;
    private long processTimeoutSecs;

    @Override
    protected void doInit(Configuration config) throws DeployerException {
        workingDir = ConfigUtils.getStringProperty(config, WORKING_DIR_CONFIG_KEY);
        command = ConfigUtils.getRequiredStringProperty(config, COMMAND_CONFIG_KEY);
        processTimeoutSecs = ConfigUtils.getLongProperty(config, PROCESS_TIMEOUT_SECS_CONFIG_KEY, DEFAULT_PROCESS_TIMEOUT_SECS);
    }

    @Override
    public void destroy() throws DeployerException {
        // Not used
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
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
