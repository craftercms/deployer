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
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} that stops the pipeline execution for a
 * given number of seconds.
 * Can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>seconds:</strong> Amount of seconds to wait</li>
 * </ul>
 *
 * @author joseross
 */
public class DelayProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DelayProcessor.class);

    protected static final String CONFIG_KEY_SECONDS = "seconds";

    // Config properties (populated in init)

    /**
     * Amount of seconds to wait
     */
    protected long seconds;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit(final Configuration config) {
        seconds = config.getLong(CONFIG_KEY_SECONDS, 10);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) {

        logger.info("Delaying pipeline execution for {} seconds", seconds);

        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            logger.warn("Could not delay pipeline execution", e);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

}
