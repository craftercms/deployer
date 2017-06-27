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
package org.craftercms.deployer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeploymentServiceException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link DeploymentService}.
 *
 * @author avasquez
 */
@Component("deploymentService")
public class DeploymentServiceImpl implements DeploymentService {

    protected final TargetService targetService;
    protected final TaskExecutor taskExecutor;

    @Autowired
    public DeploymentServiceImpl(TargetService targetService, TaskExecutor taskExecutor) {
        this.targetService = targetService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public List<Future<Deployment>> deployAllTargets(Map<String, Object> params) throws DeploymentServiceException {
        List<Target> targets;
        try {
            targets = targetService.getAllTargets();
        } catch (TargetServiceException e) {
            throw new DeploymentServiceException("Unable to retrieve list of targets", e);
        }

        List<Future<Deployment>> deployments = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(targets)) {
            for (Target target : targets) {
                FutureTask<Deployment> deployment = startDeployment(target, params);
                deployments.add(deployment);
            }
        }

        return deployments;
    }

    @Override
    public Future<Deployment> deployTarget(String env, String siteName, Map<String, Object> params)
                                throws TargetNotFoundException, DeploymentServiceException {
        try {
            Target target = targetService.getTarget(env, siteName);
            FutureTask<Deployment> deployment = startDeployment(target, params);
            return deployment;
        } catch (TargetServiceException e) {
            throw new DeploymentServiceException("Error while deploying target for env = " + env + " site = " + siteName, e);
        }
    }

    protected FutureTask<Deployment> startDeployment(Target target, Map<String, Object> params) {
        FutureTask<Deployment> task = new FutureTask<>(() -> target.deploy(params));
        taskExecutor.execute(task);
        return task;
    }

}
