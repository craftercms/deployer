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
package org.craftercms.deployer.impl.processors.aws;

import com.amazonaws.services.s3.AmazonS3;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.events.DeploymentEventsStore;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.beans.ConstructorProperties;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Implementation of {@link AbstractS3Processor} that uploads the deployment events to AWS S3.
 * Can be configured with the following YAML properties:
 *
 * <ul>
 *     <li>
 *         <strong>deploymentEventsFileUrl:</strong> URL of the deployment events file, relative to the local git repo
 *     </li>
 * </ul>
 *
 * @author joseross
 * @since 3.1.8
 */
public class S3DeploymentEventsProcessor extends AbstractS3Processor {

    protected static final String DEFAULT_DEPLOYMENT_EVENTS_FILE_URL= "deployment-events.properties";

    protected static final String CONFIG_KEY_DEPLOYMENT_EVENTS_FILE_URL = "deploymentEventsFileUrl";

    protected DeploymentEventsStore<?, Path> store;

    // Config properties (populated on init)

    /**
     * URL of the deployment events file, relative to the local git repo.
     */
    protected String deploymentEventsFileUrl;

    @ConstructorProperties({"threadPoolTaskExecutor", "store"})
    public S3DeploymentEventsProcessor(ThreadPoolTaskExecutor threadPoolTaskExecutor,
                                       DeploymentEventsStore<?, Path> store) {
        super(threadPoolTaskExecutor);
        this.store = store;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        super.doInit(config);

        deploymentEventsFileUrl = getStringProperty(config, CONFIG_KEY_DEPLOYMENT_EVENTS_FILE_URL,
                DEFAULT_DEPLOYMENT_EVENTS_FILE_URL);
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        AmazonS3 client = buildClient();
        Path file = store.getSource(deployment.getTarget());

        if (Files.exists(file)) {
            logger.info("Uploading deployment events from {}", file);
            try {
                client.putObject(s3Url.getBucket(), getS3Key(deploymentEventsFileUrl), file.toFile());
            } catch (Exception e) {
                throw new DeployerException("Error uploading deployment events @ " + file, e);
            }
        } else {
            logger.debug("No events found for target {}", targetId);
        }

        return null;
    }

}
