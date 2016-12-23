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

import org.craftercms.deployer.api.ErrorHandler;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
public class DeploymentContextImpl implements DeploymentContext {

    protected String id;
    protected DeploymentPipeline deploymentPipeline;
    protected ErrorHandler errorHandler;
    protected ConfigurableApplicationContext applicationContext;

    public DeploymentContextImpl(String id, DeploymentPipeline deploymentPipeline, ErrorHandler errorHandler,
                                 ConfigurableApplicationContext applicationContext) {
        this.id = id;
        this.deploymentPipeline = deploymentPipeline;
        this.errorHandler = errorHandler;
        this.applicationContext = applicationContext;
    }

    @Override
    public String getId() {
        return id;
    }

    public DeploymentPipeline getDeploymentPipeline() {
        return deploymentPipeline;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    @Override
    public long getDateCreated() {
        return applicationContext.getStartupDate();
    }

    @Override
    public void destroy() {
        deploymentPipeline.destroy();
        applicationContext.close();
    }

}
