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

import java.time.ZonedDateTime;

import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.TargetContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
public class TargetContextImpl implements TargetContext {

    private static final Logger logger = LoggerFactory.getLogger(TargetContextImpl.class);

    protected String id;
    protected DeploymentPipeline deploymentPipeline;
    protected DeploymentPipeline postDeploymentPipeline;
    protected ConfigurableApplicationContext applicationContext;
    protected ZonedDateTime dateCreated;

    public TargetContextImpl(String id, DeploymentPipeline deploymentPipeline, DeploymentPipeline postDeploymentPipeline,
                             ConfigurableApplicationContext applicationContext) {
        this.id = id;
        this.deploymentPipeline = deploymentPipeline;
        this.postDeploymentPipeline = postDeploymentPipeline;
        this.applicationContext = applicationContext;
        this.dateCreated = ZonedDateTime.now();
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public DeploymentPipeline getDeploymentPipeline() {
        return deploymentPipeline;
    }

    @Override
    public DeploymentPipeline getPostDeploymentPipeline() {
        return postDeploymentPipeline;
    }

    @Override
    public ZonedDateTime getDateCreated() {
        return dateCreated;
    }

    @Override
    public void destroy() {
        try {
            deploymentPipeline.destroy();
            applicationContext.close();
        } catch (Exception e) {
            logger.error("Failed to destroy target context '{}'", id);
        }
    }

}
