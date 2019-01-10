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
package org.craftercms.deployer.api;

import java.util.List;
import java.util.Map;
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
     * @param waitTillDone  if the method should wait till all deployments are done or return immediately
     * @param params        additional parameters that can be used by the deployment processors
     *
     * @return  the list of deployment info for each target
     *
     * @throws DeploymentServiceException if there was an error while executing the deployments
     */
    List<Deployment> deployAllTargets(boolean waitTillDone, Map<String, Object> params) throws DeploymentServiceException;

    /**
     * Deploys a single target
     *
     * @param env           the target's environment (e.g. dev)
     * @param siteName      the target's site name (e.g. mysite)
     * @param waitTillDone  if the method should wait till the deployment is done or return immediately
     * @param params        additional parameters that can be used by the deployment processors
     *
     * @return the deployment info
     *
     * @throws DeploymentServiceException if there was an error while executing the deployments
     */
    Deployment deployTarget(String env, String siteName, boolean waitTillDone,
                            Map<String, Object> params) throws TargetNotFoundException, DeploymentServiceException;

}
