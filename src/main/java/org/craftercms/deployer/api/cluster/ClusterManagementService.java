/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.api.cluster;

import org.craftercms.deployer.api.Target;

/**
 * Service to get cluster-related information.
 */
public interface ClusterManagementService {

    /**
     * Get the cluster mode for the given target.
     *
     * @param target the target
     * @return the cluster mode
     */
    ClusterMode getClusterMode(Target target);

    /**
     * Indicates if this instance is part of a cluster.
     *
     * @return true if this instance is part of a cluster, false otherwise
     */
    boolean isClusterOn();
}
