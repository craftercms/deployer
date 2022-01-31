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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isNoneEmpty;
import static org.craftercms.commons.config.ConfigUtils.*;

/**
 * Processor that runs a command line process.
 * A processor instance can be configured with the following YAML properties:
 *
 * <ul>
 *   <li>
 *       <strong>workingDir:</strong> The directory from which the process will run. (defaults to the deployer's folder)
 *   </li>
 *   <li>
 *       <strong>command:</strong> The full command that the process will run.
 *   </li>
 *   <li>
 *       <strong>processTimeoutSecs:</strong> The amount of seconds to wait for the process to finish. (defaults to 30)
 *   </li>
 *   <li>
 *       <strong>includeChanges:</strong> Additional parameters will be added to the command. (defaults to false)<br/>
 *       Example: script.sh SITE_NAME OPERATION (CREATE | UPDATE | DELETE) FILE (relative path of the file)
 *   </li>
 * </ul>
 *
 * @author avasquez
 */
public class CommandLineProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CommandLineProcessor.class);

    public static final String OPERATION_CREATE = "CREATE";
    public static final String OPERATION_UPDATE = "UPDATE";
    public static final String OPERATION_DELETE = "DELETE";

    protected static final String WORKING_DIR_CONFIG_KEY = "workingDir";
    protected static final String COMMAND_CONFIG_KEY = "command";
    protected static final String PROCESS_TIMEOUT_SECS_CONFIG_KEY = "processTimeoutSecs";
    protected static final String INCLUDE_CHANGES_CONFIG_KEY = "includeChanges";

    protected static final long DEFAULT_PROCESS_TIMEOUT_SECS = 30;

    // Config properties (populated on init)

    private String workingDir;
    private String command;
    private long processTimeoutSecs;
    private boolean includeChanges;

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        workingDir = getStringProperty(config, WORKING_DIR_CONFIG_KEY);
        command = getRequiredStringProperty(config, COMMAND_CONFIG_KEY);
        processTimeoutSecs = getLongProperty(config, PROCESS_TIMEOUT_SECS_CONFIG_KEY, DEFAULT_PROCESS_TIMEOUT_SECS);
        includeChanges = config.getBoolean(INCLUDE_CHANGES_CONFIG_KEY, false);
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {

        if (includeChanges) {
            String site = deployment.getTarget().getSiteName();
            processFiles(site, OPERATION_CREATE, filteredChangeSet.getCreatedFiles());
            processFiles(site, OPERATION_UPDATE, filteredChangeSet.getUpdatedFiles());
            processFiles(site, OPERATION_DELETE, filteredChangeSet.getDeletedFiles());
        } else {
            executeProcess(EMPTY, EMPTY, EMPTY);
        }

        return null;
    }

    protected void processFiles(String site, String operation, List<String> files) throws DeployerException {
        for (String file : files) {
            executeProcess(site, operation, file);
        }
    }

    protected void executeProcess(String site, String operation, String file) throws DeployerException {
        List<String> fullCommand = new LinkedList<>();

        Collections.addAll(fullCommand, command.split("\\s"));

        if (isNoneEmpty(site, operation, file)) {
            fullCommand.add(site);
            fullCommand.add(operation);
            fullCommand.add(file);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);

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

    }

}
