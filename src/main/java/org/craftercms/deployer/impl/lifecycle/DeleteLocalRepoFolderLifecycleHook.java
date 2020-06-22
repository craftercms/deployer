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
package org.craftercms.deployer.impl.lifecycle;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of {@link TargetLifecycleHook} that deletes the local Git clone of a remote repo.
 *
 * @author avasquez
 * @since 3.1.8
 */
public class DeleteLocalRepoFolderLifecycleHook extends AbstractLifecycleHook {

    protected Path localRepoFolder;

    @Required
    public void setLocalRepoFolder(String localRepoFolder) {
        this.localRepoFolder = Paths.get(localRepoFolder);
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException, DeployerException {
        // Do nothing
    }

    @Override
    protected void doExecute(Target target) throws DeployerException {
        try {
            if (Files.deleteIfExists(localRepoFolder)) {
                logger.info("Local repo folder {} deleted", localRepoFolder);
            }
        } catch (IOException e) {
            throw new DeployerException("Unable to delete local repo folder " + localRepoFolder);
        }
    }

}
