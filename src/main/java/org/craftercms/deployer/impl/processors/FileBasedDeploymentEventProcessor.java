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
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;

import static org.craftercms.deployer.utils.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.utils.ConfigUtils.getStringProperty;

/**
 * Triggers a deployment event that consumers of the repository (Crafter Engines) can subscribe to by reading a file
 * written by this processor.
 *
 * @author avasquez
 */
public class FileBasedDeploymentEventProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedDeploymentEventProcessor.class);

    protected static final String DEFAULT_DEPLOYMENT_EVENTS_FILE_URL= "deployment-events.properties";

    protected static final String CONFIG_KEY_DEPLOYMENT_EVENTS_FILE_URL = "deploymentEventsFileUrl";
    protected static final String CONFIG_KEY_EVENT_NAME = "eventName";

    /**
     * URL for the local git repository.
     */
    protected String localRepoUrl;

    // Config properties (populated on init)

    /**
     * URL of the deployment events file, relative to the local git repo.
     */
    protected String deploymentEventsFileUrl;
    /**
     * Name of the event to trigger when this processor runs.
     */
    protected String eventName;

    @Required
    public void setLocalRepoUrl(final String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException, DeployerException {
        deploymentEventsFileUrl = getStringProperty(config, CONFIG_KEY_DEPLOYMENT_EVENTS_FILE_URL,
                                                    DEFAULT_DEPLOYMENT_EVENTS_FILE_URL);
        eventName = getRequiredStringProperty(config, CONFIG_KEY_EVENT_NAME);
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        Path deploymentEventsPath = Paths.get(localRepoUrl, deploymentEventsFileUrl);
        Properties deploymentEvents = loadDeploymentEvents(deploymentEventsPath);
        boolean newFile = deploymentEvents.isEmpty();
        String now = Instant.now().toString();

        deploymentEvents.setProperty(eventName, now);

        saveDeploymentEvents(deploymentEventsPath, deploymentEvents);

        logger.info("Event {}={} saved to {}", eventName, now, deploymentEventsFileUrl);

        if (newFile) {
            originalChangeSet.addCreatedFile(deploymentEventsFileUrl);
        } else {
            originalChangeSet.addUpdatedFile(deploymentEventsFileUrl);
        }

        return originalChangeSet;
    }

    private Properties loadDeploymentEvents(Path deploymentEventsPath) throws DeployerException {
        Properties deploymentEvents = new Properties();

        if (Files.exists(deploymentEventsPath)) {
            try (Reader reader = Files.newBufferedReader(deploymentEventsPath, StandardCharsets.UTF_8)) {
                deploymentEvents.load(reader);
            } catch (IOException e) {
                throw new DeployerException("Error reading loading events file @ " + deploymentEventsPath);
            }
        }

        return deploymentEvents;
    }

    private void saveDeploymentEvents(Path deploymentEventsPath, Properties deploymentEvents) throws DeployerException {
        try (Writer writer = Files.newBufferedWriter(deploymentEventsPath, StandardCharsets.UTF_8)) {
            deploymentEvents.store(writer, null);
        } catch (IOException e) {
            throw new DeployerException("Error saving deployment events file @ " + deploymentEventsPath);
        }
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

}
