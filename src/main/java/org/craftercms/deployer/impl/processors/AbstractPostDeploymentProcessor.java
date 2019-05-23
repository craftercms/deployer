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

import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Base class for {@link org.craftercms.deployer.api.DeploymentProcessor}s that are executed during the post
 * deployment phase, which is the phase that happens after all the files from the change set have been processed.
 *
 * @author avasquez
 */
public abstract class AbstractPostDeploymentProcessor extends AbstractDeploymentProcessor {

    @Override
    public boolean isPostDeployment() {
        return true;
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ChangeSet filteredChangeSet,
                                  ChangeSet originalChangeSet) throws Exception {
        deployment.end(Deployment.Status.SUCCESS);

        return doPostProcess(deployment, filteredChangeSet, originalChangeSet);
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if there's a current jump to and it matches the label if there was a failure or the change set is not empty
        return deployment.getStatus() == Deployment.Status.FAILURE || !deployment.isChangeSetEmpty();
    }

    protected abstract ChangeSet doPostProcess(Deployment deployment, ChangeSet filteredChangeSet,
                                               ChangeSet originalChangeSet) throws DeployerException;

}
