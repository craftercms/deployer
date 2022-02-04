/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.api.events;

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Stores the deployment events for a target.
 *
 * @author joseross
 * @since 3.1.8
 * @param <T> The type to hold the events
 * @param <S> The type to store the events
 */
public interface DeploymentEventsStore<T, S> {

    /**
     * Loads the deployment events for the given target.
     * @param target the target
     * @return the existing deployment events
     * @throws DeployerException if there is any error loading the deployment events
     */
    T loadDeploymentEvents(Target target) throws DeployerException;

    /**
     * Saves the deployment events for the given target.
     * @param target the target
     * @param deploymentEvents the new deployment events
     * @throws DeployerException if there is any error saving the deployment events
     */
    void saveDeploymentEvents(Target target, T deploymentEvents) throws DeployerException;

    /**
     * Provides access to the underlaying storage object
     * @param target the target
     * @return the storage object
     */
    S getSource(Target target);

}
