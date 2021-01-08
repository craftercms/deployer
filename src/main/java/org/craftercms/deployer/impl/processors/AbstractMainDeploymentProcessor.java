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
import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Base class for {@link org.craftercms.deployer.api.DeploymentProcessor}s that are executed during the main
 * deployment phase, which is the phase where the change set is retrieved and the files are processed.
 *
 * @author avasquez
 */
public abstract class AbstractMainDeploymentProcessor extends AbstractDeploymentProcessor {

    public static final String FAIL_DEPLOYMENT_CONFIG_KEY = "failDeploymentOnFailure";

    protected boolean failDeploymentOnFailure;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(Configuration config) throws ConfigurationException, DeployerException {
        failDeploymentOnFailure = config.getBoolean(FAIL_DEPLOYMENT_CONFIG_KEY, false);

        super.init(config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChangeSet doExecute(Deployment deployment, ChangeSet filteredChangeSet,
                                  ChangeSet originalChangeSet) throws Exception {
        ProcessorExecution execution = new ProcessorExecution(name);
        deployment.addProcessorExecution(execution);

        try {
            ChangeSet newChangeSet = doMainProcess(deployment, execution, filteredChangeSet, originalChangeSet);

            execution.endExecution(Deployment.Status.SUCCESS);

            return newChangeSet;
        } catch (Exception e) {
            execution.setStatusDetails(e.toString());
            execution.endExecution(Deployment.Status.FAILURE);

            if (failDeploymentOnProcessorFailure()) {
                deployment.end(Deployment.Status.FAILURE);
            }

            throw e;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running and change set is not empty
        return deployment.isRunning() && (alwaysRun || (filteredChangeSet != null && !filteredChangeSet.isEmpty()));
    }

    /**
     * Performs the actual work of processing the files in the {@link ChangeSet}, it is also possible to return a new
     * {@link ChangeSet} to be used for the following processors in the pipeline
     *
     * @param deployment the current deployment
     * @param execution the current execution
     * @param filteredChangeSet the filtered change set (as returned by {@link #getFilteredChangeSet(ChangeSet)})
     * @param originalChangeSet the original change set (as returned by the previous processors in the pipeline)
     * @return a new {@link ChangeSet} or null
     * @throws DeployerException if there is any error processing the {@link ChangeSet}
     */
    protected abstract ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                               ChangeSet filteredChangeSet, ChangeSet originalChangeSet)
            throws DeployerException;

    /**
     * Indicates if the deployment should be marked as failed if this processor throws an error
     */
    protected boolean failDeploymentOnProcessorFailure() {
        return failDeploymentOnFailure;
    }

}
