/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.deployer.impl;

import java.util.List;

import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.DeploymentContextFactory;
import org.craftercms.deployer.api.DeploymentDetector;
import org.craftercms.deployer.api.DeploymentErrorHandler;
import org.craftercms.deployer.api.DeploymentExecutor;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.Site;
import org.craftercms.deployer.api.SiteFinder;
import org.craftercms.deployer.api.exception.DeploymentException;

/**
 * Created by alfonsovasquez on 30/11/16.
 */
public class DeploymentExecutorImpl implements DeploymentExecutor {

    protected SiteFinder siteFinder;
    protected DeploymentContextFactory deploymentContextFactory;
    protected DeploymentDetector deploymentDetector;

    @Override
    public void execute() {
        List<Site> sites = siteFinder.find();
        for (Site site : sites) {
            DeploymentContext context = deploymentContextFactory.getContext(site);
            List<DeploymentProcessor> processors = context.getProcessors();
            DeploymentErrorHandler errorHandler = context.getErrorHandler();

            try {
                ChangeSet changeSet = deploymentDetector.detectChanges(site);
                for (DeploymentProcessor processor : processors) {
                    processor.process(site, changeSet);
                }
            } catch (DeploymentException e) {
                errorHandler.handleError(site, e);
            }
        }
    }

}
