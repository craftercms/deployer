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
import java.util.Map;

import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Service that manages a target lifecycle.
 *
 * @author avasquez
 */
public interface TargetService {

    /**
     * Creates a new target with it's own configuration.
     *
     * @param env                   the target's environment (e.g. dev)
     * @param siteName              the target's site name (e.g. mysite)
     * @param replace               indicates that if there's a target with the same name, the target config should be replaced.
     * @param templateName          the name of the template used to create the target configuration.
     * @param templateParameters    the parameters that the template needs.
     *
     * @return the created target
     *
     * @throws DeployerException if an error occurred
     */
    Target createTarget(String env, String siteName, boolean replace, String templateName,
                        Map<String, Object> templateParameters) throws DeployerException;

    /**
     * Deletes a target with the given ID.
     *
     * @param env       the target's environment (e.g. dev)
     * @param siteName  the target's site name (e.g. mysite)
     *
     * @throws DeployerException if an error occurred
     */
    void deleteTarget(String env, String siteName) throws DeployerException;

    /**
     * Returns all current loaded targets
     *
     * @return the list of targets
     *
     * @throws DeployerException if an error occurred
     */
    List<Target> getAllTargets() throws DeployerException;

    /**
     * Returns the loaded target with the given ID
     *
     * @param env       the target's environment (e.g. dev)
     * @param siteName  the target's site name (e.g. mysite) 
     *
     * @return the target info
     *
     * @throws DeployerException if an error occurred
     */
    Target getTarget(String env, String siteName) throws DeployerException;

}
