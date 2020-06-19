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
package org.craftercms.deployer.impl.processors.git;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.ProcessedCommitsStore;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.craftercms.deployer.impl.DeploymentConstants.LATEST_COMMIT_ID_PARAM_NAME;

/**
 * Implementation of {@link AbstractMainDeploymentProcessor} that updates the processed commit id.
 *
 * @author joseross
 * @since 3.1.8
 */
public class GitUpdateCommitIdProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GitUpdateCommitIdProcessor.class);

    protected ProcessedCommitsStore processedCommitsStore;

    public GitUpdateCommitIdProcessor(ProcessedCommitsStore processedCommitsStore) {
        this.processedCommitsStore = processedCommitsStore;
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        ObjectId commitId = (ObjectId) deployment.getParam(LATEST_COMMIT_ID_PARAM_NAME);
        if (commitId != null) {
            logger.info("Updating processed commit to {}", commitId.name());
            processedCommitsStore.store(targetId, commitId);
        }
        return null;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException, DeployerException {
        // do nothing
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // do nothing
    }
}
