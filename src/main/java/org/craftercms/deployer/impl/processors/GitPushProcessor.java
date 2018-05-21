/*
 * Copyright (C) 2007-2018 Crafter Software Corporation. All rights reserved.
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
package org.craftercms.deployer.impl.processors;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.craftercms.deployer.api.Deployment.Status;

/**
 * Processor that pushes a localRepo to a remote Git repository. A processor instance can be configured with the
 * following YAML properties:
 *
 * <ul>
 *     <li><strong>localRepoBranch:</strong> The branch of the local repo to push.</li>
 *     <li><strong>remoteRepo.url:</strong> The URL of the remote Git repo to push to.</li>
 *     <li><strong>remoteRepo.branch:</strong> The branch of the remote Git repo to push to.</li>
 *     <li><strong>remoteRepo.username:</strong> The username for authentication with the remote Git repo.
 *     Not needed when SSH with RSA key pair authentication is used.</li>
 *     <li><strong>remoteRepo.password:</strong> The password for authentication with the remote Git repo.
 *     Not needed when SSH with RSA key pair authentication is used.</li>
 *     <li><strong>remoteRepo.ssh.privateKey.path:</strong> The SSH private key path, used only with SSH with RSA
 *     key pair authentication.</li>
 *     <li><strong>remoteRepo.ssh.privateKey.passphrase:</strong> The SSH private key passphrase, used only with
 *     SSH withRSA key pair authentication.</li>
 * </ul>
 *
 * @author avasquez
 */
public class GitPushProcessor extends AbstractRemoteGitRepoAwareProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GitPushProcessor.class);

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
        File gitFolder = new File(localRepoFolder, GitUtils.GIT_FOLDER_NAME);

        if (localRepoFolder.exists() && gitFolder.exists()) {
            doPush(execution);
        } else {
        	logger.warn("No local git repository @ {}", localRepoFolder);
        }

        return null;
    }

    protected void doPush(ProcessorExecution execution) throws DeployerException {
        try (Git git = openLocalRepository()) {
            logger.info("Executing git push for repository {}...", localRepoFolder);

            Iterable<PushResult> pushResults = GitUtils.push(git, remoteRepoUrl, remoteRepoBranch,
                                                             authenticationConfigurator);
            List<String> detailsList = new ArrayList<>();

            boolean success = checkPushResults(pushResults, detailsList);

            if (CollectionUtils.isNotEmpty(detailsList)) {
                execution.setStatusDetails(detailsList);

                if (!success) {
                    execution.endExecution(Status.FAILURE);
                }
            } else {
                execution.setStatusDetails("Not push result returned after pull operation");
            }
        } catch (GitAPIException e) {
            throw new DeployerException("Execution of git push failed", e);
        }
    }

    protected boolean checkPushResults(Iterable<PushResult> pushResults,
                                       List<String> detailList) throws DeployerException {
        boolean success = true;

        if (pushResults != null) {
            for (PushResult result : pushResults) {
                Collection<RemoteRefUpdate> remoteRefUpdates = result.getRemoteUpdates();
                if (CollectionUtils.isNotEmpty(remoteRefUpdates)) {
                    for (RemoteRefUpdate update : remoteRefUpdates) {
                        if (!checkRemoteRefUpdate(update, detailList)) {
                            success = false;
                        }
                    }
                }
            }
        }

        return success;
    }

    protected boolean checkRemoteRefUpdate(RemoteRefUpdate update, List<String> detailList) throws DeployerException {
        RemoteRefUpdate.Status status = update.getStatus();
        String updatedBranch = update.getRemoteName();
        String details;

        switch (status) {
            case OK:
                details = "Branch '" + updatedBranch + "' of remote repo " + remoteRepoUrl + " updated " +
                          "successfully (update with status " + status + ")";
                detailList.add(details);

                logger.info(details);

                return true;
            case UP_TO_DATE:
                details = "Branch '" + updatedBranch + "' of remote repo " + remoteRepoUrl + " already up " +
                          "to date (update with status " + status + ")";
                detailList.add(details);

                logger.info(details);

                return true;
            default:
                // Non-supported push results
                details = "Received unexpected result after executing push: " + status;
                detailList.add(details);

                logger.error(details);

                return false;
        }
    }

}
