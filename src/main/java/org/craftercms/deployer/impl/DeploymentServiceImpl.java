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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.TargetContext;
import org.craftercms.deployer.api.TargetResolver;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by alfonsovasquez on 30/11/16.
 */
@Component("deploymentService")
public class DeploymentServiceImpl implements DeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    protected final TargetResolver targetResolver;

    @Autowired
    public DeploymentServiceImpl(TargetResolver targetResolver) {
        this.targetResolver = targetResolver;
    }

    @Override
    public  List<Deployment> deployAllSites() throws DeploymentException {
        List<TargetContext> targetContexts = targetResolver.resolveAll();
        List<Deployment> deployments = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(targetContexts)) {
            for (TargetContext context : targetContexts) {
                Deployment deployment = deploySite(context);

                deployments.add(deployment);
            };
        }

        return deployments;
    }

    public Deployment deploySite(TargetContext context) {
        Deployment deployment = new Deployment(context);

        logger.info("**************************************************");
        logger.info("* Deployment pipeline  for '{}' started", context.getId());
        logger.info("**************************************************");

        context.getDeploymentPipeline().execute(deployment);

        deployment.endDeployment(Deployment.Status.SUCCESS);

        logger.info("**************************************************");
        logger.info("* Deployment pipeline for '{}' finished", context.getId());
        logger.info("**************************************************");

        return deployment;
    }

}
