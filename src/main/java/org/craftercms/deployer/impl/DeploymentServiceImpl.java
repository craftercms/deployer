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
package org.craftercms.deployer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeploymentServiceException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetNotReadyException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link DeploymentService}.
 *
 * @author avasquez
 */
@Component("deploymentService")
public class DeploymentServiceImpl implements DeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    protected final TargetService targetService;

    @Autowired
    public DeploymentServiceImpl(TargetService targetService) {
        this.targetService = targetService;
    }

    @Override
    public List<Deployment> deployAllTargets(boolean waitTillDone,
                                             Map<String, Object> params) throws DeploymentServiceException {
        List<Target> targets;
        try {
            targets = targetService.getAllTargets();
        } catch (TargetServiceException e) {
            throw new DeploymentServiceException("Unable to retrieve list of targets", e);
        }

        List<Deployment> deployments = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(targets)) {
            for (Target target : targets) {
                Deployment deployment;
                try {
                    deployment = target.deploy(waitTillDone, params);
                    deployments.add(deployment);
                } catch (TargetNotReadyException e) {
                    logger.error(e.getMessage());
                }
            }
        }

        return deployments;
    }

    @Override
    public Deployment deployTarget(String env, String siteName, boolean waitTillDone,
                                   Map<String, Object> params) throws TargetNotFoundException,
                                                                      DeploymentServiceException {
        try {
            return targetService.getTarget(env, siteName).deploy(waitTillDone, params);
        } catch (TargetServiceException | TargetNotReadyException e) {
            throw new DeploymentServiceException("Error while deploying target '" + TargetImpl.getId(env, siteName) +
                                                 "'", e);
        }
    }

}
