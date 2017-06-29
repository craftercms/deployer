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
package org.craftercms.deployer.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.craftercms.deployer.api.exceptions.DeploymentServiceException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;

/**
 * Service for doing deployments.
 *
 * @author avasquez
 */
public interface DeploymentService {

    /**
     * Deploys all targets
     *
     * @param params additional parameters that can be used by the deployment processors
     *
     * @return  the list of deployment info for each target
     *
     * @throws DeploymentServiceException if there was an error while executing the deployments
     */
    List<Future<Deployment>> deployAllTargets(Map<String, Object> params) throws DeploymentServiceException;

    /**
     * Deploys a single target
     *
     * @param env       the target's environment (e.g. dev)
     * @param siteName  the target's site name (e.g. mysite)
     * @param params    additional parameters that can be used by the deployment processors
     *
     * @return the deployment info
     *
     * @throws DeploymentServiceException if there was an error while executing the deployments
     */
    Future<Deployment> deployTarget(String env, String siteName,
                            Map<String, Object> params) throws TargetNotFoundException, DeploymentServiceException;

}
