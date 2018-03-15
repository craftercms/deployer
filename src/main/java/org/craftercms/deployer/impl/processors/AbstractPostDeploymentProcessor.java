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

import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link org.craftercms.deployer.api.DeploymentProcessor}s that are executed during the post deployment phase, which is
 * the phase that happens after all the files from the change set have been processed.
 *
 * @author avasquez
 */
public abstract class AbstractPostDeploymentProcessor extends AbstractDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPostDeploymentProcessor.class);

    @Override
    public void execute(Deployment deployment) {
        deployment.end(Deployment.Status.SUCCESS);

        if (shouldExecute(deployment)) {
            try {
                logger.info("----- {} @ {} -----", name, targetId);

                doExecute(deployment);
            } catch (Exception e) {
                logger.error("Processor '" + name + "' for target '" + targetId + "' failed", e);
            }
        }
    }

    protected boolean shouldExecute(Deployment deployment) {
        // Run if there was a failure or the change set is not empty
        return deployment.getStatus() == Deployment.Status.FAILURE || !deployment.isChangeSetEmpty();
    }

    protected abstract void doExecute(Deployment deployment) throws DeployerException;

}
