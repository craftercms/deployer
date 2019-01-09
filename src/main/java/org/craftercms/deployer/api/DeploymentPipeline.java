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
import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Represents a collection of processors that are executed on each deployment.
 *
 * @author avasquez
 */
public interface DeploymentPipeline {

    /**
     * Destroys the pipeline, calling destroy also on all its processors
     *
     * @throws DeployerException if an error occurs
     */
    void destroy() throws DeployerException;

    /**
     * Returns the processors that make up this pipeline. The returned list is unmodifiable.
     */
    List<DeploymentProcessor> getProcessors();

    /**
     * Does a deployment.
     *
     * @param deployment    the deployment info
     */
    void execute(Deployment deployment);

}
