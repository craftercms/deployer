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

import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.beans.InitializableByConfigBean;

/**
 * Represents a single deployment processor.
 *
 * @author avasquez
 */
public interface DeploymentProcessor extends InitializableByConfigBean {

    /**
     * Returns true if this processor runs after the deployment has finalized.
     */
    boolean isPostDeployment();

    /**
     * Destroys the processor, closing and releasing any used resources.
     *
     * @throws DeployerException if an error occurs
     */
    void destroy() throws DeployerException;

    /**
     * Executes the processor
     *
     * @param deployment    the current deployment info
     */
    void execute(Deployment deployment);

}
