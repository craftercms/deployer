/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
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

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.beans.InitializableByConfigBean;

/**
 * A hook executed during a lifecycle phase of a target. Current valid lifecycle phases:
 *
 * <ul>
 *     <li>Create: when the create target API is called</li>
 *     <li>Init: after a create or after the config is loaded</li>
 *     <li>Delete: when the delete target API is called</li>
 * </ul>
 *
 * @author avasquez
 */
public interface TargetLifecycleHook extends InitializableByConfigBean {

    /**
     * Execute the hook.
     *
     * @param target the target associated to the hook
     * @throws DeployerException if there's an exception on execution
     */
    void execute(Target target) throws DeployerException;

}
