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
package org.craftercms.deployer.api.lifecycle;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * A hook executed during a lifecycle phase of a target. Current valid lifecycle phases:
 *
 * <ul>
 *     <li>Create</li>
 *     <li>Delete</li>
 * </ul>
 *
 * @author avasquez
 */
public interface TargetLifecycleHook {

    /**
     * Initializes the hook, configuring it by using the specified configuration.
     *
     * @param config the processor configuration
     *
     * @throws ConfigurationException if there's configuration related exception
     * @throws DeployerException if there's a general exception on init
     */
    void init(Configuration config) throws ConfigurationException, DeployerException;

    /**
     * Execute the hook.
     *
     * @param target the target associated to the hook
     * @throws DeployerException if there's an exception on execution
     */
    void execute(Target target) throws DeployerException;

}
