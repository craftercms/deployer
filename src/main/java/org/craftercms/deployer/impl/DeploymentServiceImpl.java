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
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by alfonsovasquez on 30/11/16.
 */
@Component("deploymentService")
public class DeploymentServiceImpl implements DeploymentService {

    protected final TargetService targetService;

    @Autowired
    public DeploymentServiceImpl(TargetService targetService) {
        this.targetService = targetService;
    }

    @Override
    public List<Deployment> deployAllTargets() throws DeployerException {
        List<Target> targets = targetService.getAllTargets();
        List<Deployment> deployments = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(targets)) {
            for (Target target : targets) {
                Deployment deployment = target.deploy();
                deployments.add(deployment);
            };
        }

        return deployments;
    }

    @Override
    public Deployment deployTarget(String env, String siteName) throws DeployerException {
        return targetService.getTarget(env, siteName).deploy();
    }

}
