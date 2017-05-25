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

import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Represents a single deployment processor.
 *
 * @author avasquez
 */
public interface DeploymentProcessor {

    /**
     * Initializes the processor, configuring it by using the specified configuration.
     *
     * @param config    the processor configuration
     *
     * @throws DeployerException if a configuration property is missing or if any other error occurred
     */
    void init(Configuration config) throws DeployerException;

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
     * @param params        additional parameters that can be used by the processor
     */
    void execute(Deployment deployment, Map<String, Object> params);

}
