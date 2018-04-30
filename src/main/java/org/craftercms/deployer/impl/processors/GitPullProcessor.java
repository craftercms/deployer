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
import java.io.IOException;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.git.auth.BasicUsernamePasswordAuthConfigurator;
import org.craftercms.commons.git.auth.GitAuthenticationConfigurator;
import org.craftercms.commons.git.auth.SshRsaKeyPairAuthConfigurator;
import org.craftercms.commons.git.auth.SshUsernamePasswordAuthConfigurator;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.craftercms.deployer.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Processor that clones/pulls a remote Git repository into a local path in the filesystem. It uses a
 * {@link GitAuthenticationConfigurator}
 * to configure the Git connection authentication. A processor instance can be configured with the following YAML
 * properties:
 * <p>
 * <ul>
 * <li><strong>remoteRepo.url:</strong> The URL of the remote Git repo to clone/pull</li>
 * <li><strong>remoteRepo.branch:</strong> The branch of the remote Git repo to clone/pull</li>
 * <li><strong>remoteRepo.username:</strong> The username for authentication with the remote Git repo. Not needed when
 * SSH with RSA key pair authentication is used.</li>
 * <li><strong>remoteRepo.password:</strong> The password for authentication with the remote Git repo. Not needed when
 * SSH with RSA key pair authentication is used.</li>
 * <li><strong>remoteRepo.ssh.privateKey.path:</strong> The SSH private key path, used only with SSH with RSA key pair
 * authentication.</li>
 * <li><strong>remoteRepo.ssh.privateKey.passphrase:</strong> The SSH private key passphrase, used only with SSH with
 * RSA key pair authentication.</li>
 * </ul>
 *
 * @author avasquez
 */
public class GitPullProcessor extends AbstractMainDeploymentProcessor {

    public static final String REMOTE_REPO_URL_CONFIG_KEY = "remoteRepo.url";
    public static final String REMOTE_REPO_BRANCH_CONFIG_KEY = "remoteRepo.branch";
    public static final String REMOTE_REPO_USERNAME_CONFIG_KEY = "remoteRepo.username";
    public static final String REMOTE_REPO_PASSWORD_CONFIG_KEY = "remoteRepo.password";
    public static final String REMOTE_REPO_SSH_PRV_KEY_PATH_CONFIG_KEY = "remoteRepo.ssh.privateKey.path";
    public static final String REMOTE_REPO_SSH_PRV_KEY_PASSPHRASE_CONFIG_KEY = "remoteRepo.ssh.privateKey.passphrase";

    public static final String GIT_FOLDER_NAME = ".git";

    private static final Logger logger = LoggerFactory.getLogger(GitPullProcessor.class);

    protected File localRepoFolder;

    protected String remoteRepoUrl;
    protected String remoteRepoBranch;
    protected GitAuthenticationConfigurator authenticationConfigurator;

    /**
     * Sets the local filesystem folder that will contain the remote repo clone.
     */
    @Required
    public void setLocalRepoFolder(File localRepoFolder) {
        this.localRepoFolder = localRepoFolder;
    }

    @Override
    protected void doInit(Configuration config) throws DeployerException {
        remoteRepoUrl = ConfigUtils.getRequiredStringProperty(config, REMOTE_REPO_URL_CONFIG_KEY);
        remoteRepoBranch = ConfigUtils.getStringProperty(config, REMOTE_REPO_BRANCH_CONFIG_KEY);
        authenticationConfigurator = createAuthenticationConfigurator(config, remoteRepoUrl);
    }

    @Override
    public void destroy() throws DeployerException {
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running
        return deployment.isRunning();
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
        File gitFolder = new File(localRepoFolder, GIT_FOLDER_NAME);

        if (localRepoFolder.exists() && gitFolder.exists()) {
            doPull(execution);
        } else {
            doClone(execution);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

    protected void doPull(ProcessorExecution execution) throws DeployerException {
        try (Git git = openLocalRepository()) {
            logger.info("Executing git pull for repository {}...", localRepoFolder);

            PullResult pullResult = GitUtils.pull(git, remoteRepoBranch, MergeStrategy.THEIRS,
                                                  authenticationConfigurator);
            String details;

            if (pullResult != null && pullResult.getMergeResult() != null) {
                details = checkMergeResult(pullResult.getMergeResult());
            } else {
                details = "No pull or merge result returned after pull operation";
            }

            logger.info(details);

            execution.setStatusDetails(details);
        } catch (GitAPIException e) {
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
                    throw new DeployerException("Received unsupported merge result after executing pull: " + status);
            }
        } else {
            throw new DeployerException("Merge failed with status " + status);
        }
    }

    protected Git openLocalRepository() throws DeployerException {
        try {
            logger.debug("Opening local Git repository at {}", localRepoFolder);

            return GitUtils.openRepository(localRepoFolder);
        } catch (IOException e) {
            throw new DeployerException("Failed to open Git repository at " + localRepoFolder, e);
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

            return GitUtils.cloneRemoteRepository(remoteRepoUrl, remoteRepoBranch, authenticationConfigurator,
                                                  localRepoFolder, null, null, null);
        } catch (IOException | GitAPIException | IllegalArgumentException e) {
            // Force delete so there's no invalid remains
            FileUtils.deleteQuietly(localRepoFolder);

            throw new DeployerException(
                "Failed to clone Git remote repository " + remoteRepoUrl + " into " + localRepoFolder, e);
        }
    }

    protected GitAuthenticationConfigurator createAuthenticationConfigurator(Configuration config,
                                                                             String repoUrl) throws
        DeployerConfigurationException {
        GitAuthenticationConfigurator authConfigurator = null;

        if (repoUrl.startsWith("ssh:")) {
            String password = ConfigUtils.getStringProperty(config, REMOTE_REPO_PASSWORD_CONFIG_KEY);

            if (StringUtils.isNotEmpty(password)) {
                logger.debug("SSH username/password authentication will be used to connect to repo {}", repoUrl);

                authConfigurator = new SshUsernamePasswordAuthConfigurator(password);
            } else {
                String privateKeyPath = ConfigUtils.getStringProperty(config, REMOTE_REPO_SSH_PRV_KEY_PATH_CONFIG_KEY);
                String passphrase = ConfigUtils.getStringProperty(config,
                                                                  REMOTE_REPO_SSH_PRV_KEY_PASSPHRASE_CONFIG_KEY);

                logger.debug("SSH RSA key pair authentication will be used to connect to repo {}", repoUrl);

                SshRsaKeyPairAuthConfigurator keyPairAuthConfigurator = new SshRsaKeyPairAuthConfigurator();
                keyPairAuthConfigurator.setPrivateKeyPath(privateKeyPath);
                keyPairAuthConfigurator.setPassphrase(passphrase);

                authConfigurator = keyPairAuthConfigurator;
            }
        } else {
            String username = ConfigUtils.getStringProperty(config, REMOTE_REPO_USERNAME_CONFIG_KEY);
            String password = ConfigUtils.getStringProperty(config, REMOTE_REPO_PASSWORD_CONFIG_KEY);

            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                logger.debug("Username/password authentication will be used to connect to repo {}", repoUrl);

                authConfigurator = new BasicUsernamePasswordAuthConfigurator(username, password);
            }
        }

        return authConfigurator;
    }

}
