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
package org.craftercms.deployer.impl;

import org.craftercms.deployer.api.exceptions.DeployerException;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Store that holds the last processed Git commit for each target.
 *
 * @author avasquez
 */
public interface ProcessedCommitsStore {

    /**
     * Loads the stored commit ID for the specified target.
     *
     * @param targetId the target's ID
     *
     * @return the commit's {@code ObjectId}, or null if not found
     *
     * @throws DeployerException if an error occurs
     */
    ObjectId load(String targetId) throws DeployerException;

    /**
     * Stores the specified commit ID for the target.
     *
     * @param targetId  the target's ID
     * @param commitId  the commit's {@code ObjectId}
     *
     * @throws DeployerException if an error occurs
     */
    void store(String targetId, ObjectId commitId) throws DeployerException;

    /**
     * Deletes the stored commit ID for the specified target.
     *
     * @param targetId the target's ID
     *
     * @throws DeployerException if an error occurs
     */
    void delete(String targetId) throws DeployerException;

}
