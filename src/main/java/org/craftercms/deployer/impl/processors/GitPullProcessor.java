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
package org.craftercms.deployer.impl.processors;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.merge.MergeStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Processor that clones/pulls a remote Git repository into a local path in the filesystem. A processor instance
 * can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>remoteRepo.url:</strong> The URL of the remote Git repo to pull.</li>
 *     <li><strong>remoteRepo.name:</strong> The name to use for the remote repo when pulling from it (origin by default).</li>
 *     <li><strong>remoteRepo.branch:</strong> The branch of the remote Git repo to pull.</li>
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
public class GitPullProcessor extends AbstractRemoteGitRepoAwareProcessor {

    protected static final String REMOTE_REPO_NAME_CONFIG_KEY = "remoteRepo.name";

    private static final Logger logger = LoggerFactory.getLogger(GitPullProcessor.class);

    // Config properties (populated on init)

    protected String remoteRepoName;

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        super.doInit(config);

        remoteRepoName = getStringProperty(config, REMOTE_REPO_NAME_CONFIG_KEY, Constants.DEFAULT_REMOTE_NAME);
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        File gitFolder = new File(localRepoFolder, GitUtils.GIT_FOLDER_NAME);

        if (localRepoFolder.exists() && gitFolder.exists()) {
            doPull(execution);
        } else {
            doClone(execution);
        }

        return null;
    }

    protected void doPull(ProcessorExecution execution) throws DeployerException {
        try (Git git = openLocalRepository()) {
            logger.info("Executing git pull for repository {}...", localRepoFolder);

            GitUtils.discardAllChanges(git);

            PullResult pullResult = GitUtils.pull(git, remoteRepoName, remoteRepoUrl, remoteRepoBranch,
                                                  MergeStrategy.THEIRS, authenticationConfigurator);
            String details;

            if (pullResult != null && pullResult.getMergeResult() != null) {
                details = checkMergeResult(pullResult.getMergeResult());
            } else {
                details = "No pull or merge result returned after pull operation";
            }

            logger.info(details);

            execution.setStatusDetails(details);
        } catch (GitAPIException | URISyntaxException e) {
            throw new DeployerException("Execution of git pull failed:", e);
        }
    }

    protected String checkMergeResult(MergeResult mergeResult) throws DeployerException {
        MergeResult.MergeStatus status = mergeResult.getMergeStatus();
        if (status.isSuccessful()) {
            switch (status) {
                case FAST_FORWARD:
                case MERGED:
                    return "Changes successfully pulled from remote repo " + remoteRepoUrl + " into local repo " +
                           localRepoFolder + " (merge result with status " + status + ")";
                case ALREADY_UP_TO_DATE:
                    return "Local repository " + localRepoFolder + " up to date (no changes pulled from remote repo " +
                           remoteRepoUrl + ") (merge result with status " + status + ")";
                default:
                    // Non-supported merge results
                    throw new DeployerException("Received unexpected merge result after executing pull: " + status);
            }
        } else {
            throw new DeployerException("Merge failed with status " + status);
        }
    }

    protected void doClone(ProcessorExecution execution) throws DeployerException {
        try (Git git = cloneRemoteRepository()) {
            String details = "Successfully cloned Git remote repository " + remoteRepoUrl + " into " + localRepoFolder;

            logger.info(details);

            execution.setStatusDetails(details);
        }
    }

    protected Git cloneRemoteRepository() throws DeployerException {
        try {
            if (localRepoFolder.exists()) {
                logger.debug("Deleting existing folder {} before cloning", localRepoFolder);

                FileUtils.forceDelete(localRepoFolder);
            } else {
                logger.debug("Creating folder {} and any nonexistent parents before cloning", localRepoFolder);

                FileUtils.forceMkdir(localRepoFolder);
            }

            logger.info("Cloning Git remote repository {} into {}", remoteRepoUrl, localRepoFolder);

            return GitUtils.cloneRemoteRepository(remoteRepoName, remoteRepoUrl, remoteRepoBranch,
                                                  authenticationConfigurator, localRepoFolder, null,
                                                  null, null);
        } catch (IOException | GitAPIException | IllegalArgumentException e) {
            // Force delete so there's no invalid remains
            FileUtils.deleteQuietly(localRepoFolder);

            throw new DeployerException(
                "Failed to clone Git remote repository " + remoteRepoUrl + " into " + localRepoFolder, e);
        }
    }

}
