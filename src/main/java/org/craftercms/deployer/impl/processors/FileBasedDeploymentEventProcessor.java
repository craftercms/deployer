/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.events.DeploymentEventsStore;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.ConstructorProperties;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Triggers a deployment event that consumers of the repository (Crafter Engines) can subscribe to.
 *
 * @author avasquez
 */
public class FileBasedDeploymentEventProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FileBasedDeploymentEventProcessor.class);

    protected static final String CONFIG_KEY_EVENT_NAME = "eventName";

    protected DeploymentEventsStore<Properties, Path> store;

    // Config properties (populated on init)

    /**
     * URL of the deployment events file, relative to the local git repo.
     */
    protected String deploymentEventsFileUrl;
    /**
     * Name of the event to trigger when this processor runs.
     */
    protected String eventName;

    @ConstructorProperties({"store"})
    public FileBasedDeploymentEventProcessor(DeploymentEventsStore<Properties, Path> store) {
        this.store = store;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException, DeployerException {
        eventName = getRequiredStringProperty(config, CONFIG_KEY_EVENT_NAME);
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        Target target = deployment.getTarget();
        Properties deploymentEvents = store.loadDeploymentEvents(target);
        String now = Instant.now().toString();

        deploymentEvents.setProperty(eventName, now);

        store.saveDeploymentEvents(target, deploymentEvents);

        logger.info("Event {}={} saved to {}", eventName, now, store.getSource(target));

        return null;
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

}
