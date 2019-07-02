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

import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;

/**
 * Service that manages targets.
 *
 * @author avasquez
 */
public interface TargetService {


    /**
     * Returns true if the target associated to the env and site name exists.
     *
     * @param env       the target's environment (e.g. dev)
     * @param siteName  the target's site name (e.g. mysite)
     *
     * @return true if the target exists, false otherwise
     * @throws TargetServiceException if a general error occurs
     */
    boolean targetExists(String env, String siteName) throws TargetServiceException;

    /**
     * Creates a new target with it's own configuration. Creating a target also triggers its create lifecycle hooks.
     *
     * @param env               the target's environment (e.g. dev)
     * @param siteName          the target's site name (e.g. mysite)
     * @param replace           indicates that if there's a target with the same name, the target config should be
     *                          replaced.
     * @param templateName      the name of the template used to create the target configuration (can be null).
     * @param templateParams    the parameters that the template needs.
     *
     * @return the created target
     *
     * @throws TargetAlreadyExistsException if the target for the specified env and site name already exists
     * @throws TargetServiceException if a general error occurs
     */
    Target createTarget(String env, String siteName, boolean replace, String templateName, boolean crafterSearchEnabled,
                        Map<String, Object> templateParams) throws TargetAlreadyExistsException, TargetServiceException;

    /**
     * Deletes a target with the given ID. Deleting the target also triggers its delete lifecycle hooks.
     *
     * @param env       the target's environment (e.g. dev)
     * @param siteName  the target's site name (e.g. mysite)
     *
     * @throws TargetNotFoundException if the target for the specified env and site name doesn't exist
     * @throws TargetServiceException if a general error occurs
     */
    void deleteTarget(String env, String siteName) throws TargetNotFoundException, TargetServiceException;

    /**
     * Scans for target configurations, loading targets with new/modified configuration and unloading targets with
     * no configuration. This method triggers no lifecycle hooks.
     *
     * @return existing targets, after being loaded
     *
     * @throws TargetServiceException if a general error occurs
     */
    List<Target> resolveTargets() throws TargetServiceException;

    /**
     * Returns all targets.
     *
     * @return the list of targets
     *
     * @throws TargetServiceException if a general error occurs
     */
    List<Target> getAllTargets() throws TargetServiceException;

    /**
     * Returns the current target with the given ID
     *
     * @param env       the target's environment (e.g. dev)
     * @param siteName  the target's site name (e.g. mysite) 
     *
     * @return the target info
     *
     * @throws TargetNotFoundException if the target for the specified env and site name doesn't exist
     * @throws TargetServiceException if a general error occurs
     */
    Target getTarget(String env, String siteName) throws TargetNotFoundException, TargetServiceException;

}
