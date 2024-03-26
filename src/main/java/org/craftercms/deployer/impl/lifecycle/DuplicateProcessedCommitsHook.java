/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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
import org.craftercms.deployer.impl.ProcessedCommitsStore;
import org.craftercms.deployer.impl.TargetImpl;
import org.eclipse.jgit.lib.ObjectId;

import java.beans.ConstructorProperties;

/**
 * Implementation of {@link org.craftercms.deployer.api.lifecycle.TargetLifecycleHook} that duplicates the processed-commits file
 * from a source site to a target site.
 */
public class DuplicateProcessedCommitsHook extends AbstractLifecycleHook {
    private final String siteName;
    private final String sourceSiteName;
    private final String env;
    private final ProcessedCommitsStore processedCommitsStore;

    @ConstructorProperties({"siteName", "sourceSiteName", "env", "processedCommitsStore"})
    public DuplicateProcessedCommitsHook(final String siteName, final String sourceSiteName, final String env, final ProcessedCommitsStore processedCommitsStore) {
        this.siteName = siteName;
        this.sourceSiteName = sourceSiteName;
        this.env = env;
        this.processedCommitsStore = processedCommitsStore;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException, DeployerException {
    }

    @Override
    protected void doExecute(Target target) throws DeployerException {
        logger.info("Starting processed-commits file duplicate from site '{}' to site '{}'", sourceSiteName, siteName);
        String srcTargetId = TargetImpl.getId(env, sourceSiteName);
        String newTargetId = TargetImpl.getId(env, siteName);
        ObjectId processedCommit = processedCommitsStore.load(srcTargetId);
        processedCommitsStore.store(newTargetId, processedCommit);
        logger.info("Completed processed-commits file duplicate from site '{}' to site '{}'", sourceSiteName, siteName);
    }
}
