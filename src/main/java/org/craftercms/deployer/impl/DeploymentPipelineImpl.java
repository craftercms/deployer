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
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alfonsovasquez on 12/18/16.
 */
public class DeploymentPipelineImpl implements DeploymentPipeline {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    protected List<DeploymentProcessor> processors;

    public DeploymentPipelineImpl(List<DeploymentProcessor> processors) {
        this.processors = processors;
    }

    @Override
    public void destroy() throws DeployerException {
        if (CollectionUtils.isNotEmpty(processors)) {
            for (DeploymentProcessor processor : processors) {
                try {
                    processor.destroy();
                } catch (Exception e) {
                    logger.error("Failed to destroy processor " + processor, e);
                }
            }
        }
    }

    @Override
    public Deployment execute(Target target, Map<String, Object> params) {
        Deployment deployment = new Deployment(target);

        executeProcessors(deployment, params);

        deployment.endDeployment(Deployment.Status.SUCCESS);

        return deployment;
    }

    protected void executeProcessors(Deployment deployment, Map<String, Object> params) {
        if (CollectionUtils.isNotEmpty(processors)) {
            for (DeploymentProcessor processor : processors) {
                processor.execute(deployment, params);
            }
        }
    }

}
