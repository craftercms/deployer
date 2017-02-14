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
package org.craftercms.deployer.api;

import java.util.List;

import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Service for doing deployments.
 *
 * @author avasquez
 */
public interface DeploymentService {

    /**
     * Deploys all targets
     *
     * @return  the list of deployment info for each target
     *
     * @throws DeployerException if there was an error while doing the deployments
     */
    List<Deployment> deployAllTargets() throws DeployerException;

    /**
     * Deploys a single target
     *
     * @param env       the target's environment (e.g. dev)
     * @param siteName  the target's site name (e.g. mysite) 
     *
     * @return the deployment info
     */
    Deployment deployTarget(String env, String siteName) throws DeployerException;

}
