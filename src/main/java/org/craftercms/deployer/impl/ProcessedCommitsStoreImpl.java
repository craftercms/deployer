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
package org.craftercms.deployer.impl;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSED_COMMIT_FILE_EXTENSION;

/**
 * Default implementation of {@link ProcessedCommitsStore} that stores each commit ID in a file, and all commit ID files are stored
 * in a certain location in the filesystem.
 *
 * @author avasquez
 */
public class ProcessedCommitsStoreImpl implements ProcessedCommitsStore {

    private static final Logger logger = LoggerFactory.getLogger(ProcessedCommitsStoreImpl.class);

    protected File storeFolder;

    @Required
    public void setStoreFolder(File storeFolder) {
        this.storeFolder = storeFolder;
    }

    @Override
    public ObjectId load(String targetId) throws DeployerException {
        File commitFile = getCommitFile(targetId);
        try {
            if (commitFile.exists()) {
                String commitId = FileUtils.readFileToString(commitFile, "UTF-8").trim();
                if (StringUtils.isNotEmpty(commitId)) {
                    logger.debug("Found previous processed commit ID for target '{}': {}", targetId, commitId);

                    return ObjectId.fromString(commitId);
                } else {
                    logger.warn("Processed commit file {} is empty, will be deleted", commitFile);

                    FileUtils.deleteQuietly(commitFile);

                    return null;
                }
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new DeployerException("Error retrieving previous processed commit ID from " + commitFile, e);
        }
    }

    @Override
    public void store(String targetId, ObjectId commitId) throws DeployerException {
        File commitFile = getCommitFile(targetId);
        try {
            logger.debug("Storing processed commit ID {} for target '{}'", commitId.name(), targetId);

            FileUtils.write(commitFile, commitId.name(), "UTF-8", false);
        } catch (IOException e) {
            throw new DeployerException("Error saving processed commit ID to " + commitFile, e);
        }
    }

    @Override
    public void delete(String targetId) throws DeployerException {
        File commitFile = getCommitFile(targetId);
        if (commitFile.exists()) {
            logger.debug("Deleting processed commit from store for target '{}'", targetId);

            FileUtils.deleteQuietly(commitFile);
        }
    }

    protected File getCommitFile(String targetId) {
        return new File(storeFolder, targetId + "." + PROCESSED_COMMIT_FILE_EXTENSION);
    }

}
