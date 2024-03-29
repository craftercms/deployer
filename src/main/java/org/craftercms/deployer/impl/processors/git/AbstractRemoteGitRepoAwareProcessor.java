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
package org.craftercms.deployer.impl.processors.git;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.git.auth.GitAuthenticationConfigurator;
import org.craftercms.commons.git.utils.AuthConfiguratorFactory;
import org.craftercms.commons.git.utils.GitUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Base class for processors that work against a remote repo. It basically provides the code that is used to
 * authenticate to the remote repository. A processor instance can be configured with the following YAML
 * properties:
 *
 * <ul>
 *     <li><strong>remoteRepo.url:</strong> The URL of the remote Git repo.</li>
 *     <li><strong>remoteRepo.branch:</strong> The branch of the remote Git repo.</li>
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
public abstract class AbstractRemoteGitRepoAwareProcessor extends AbstractMainDeploymentProcessor {

    protected static final String REMOTE_REPO_URL_CONFIG_KEY = "remoteRepo.url";
    protected static final String REMOTE_REPO_BRANCH_CONFIG_KEY = "remoteRepo.branch";
    protected static final String REMOTE_REPO_USERNAME_CONFIG_KEY = "remoteRepo.username";
    protected static final String REMOTE_REPO_PASSWORD_CONFIG_KEY = "remoteRepo.password";
    protected static final String REMOTE_REPO_SSH_PRV_KEY_PATH_CONFIG_KEY = "remoteRepo.ssh.privateKey.path";
    protected static final String REMOTE_REPO_SSH_PRV_KEY_PASSPHRASE_CONFIG_KEY = "remoteRepo.ssh.privateKey.passphrase";

    private static final Logger logger = LoggerFactory.getLogger(AbstractRemoteGitRepoAwareProcessor.class);

    protected File localRepoFolder;

    // Config properties (populated on init)

    protected String remoteRepoUrl;
    protected String remoteRepoBranch;
    protected GitAuthenticationConfigurator authenticationConfigurator;
    protected AuthConfiguratorFactory authConfiguratorFactory;

    public AbstractRemoteGitRepoAwareProcessor(File localRepoFolder,
                                               AuthConfiguratorFactory authConfiguratorFactory) {
        this.localRepoFolder = localRepoFolder;
        this.authConfiguratorFactory = authConfiguratorFactory;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        remoteRepoUrl = getRequiredStringProperty(config, REMOTE_REPO_URL_CONFIG_KEY);
        remoteRepoBranch = getStringProperty(config, REMOTE_REPO_BRANCH_CONFIG_KEY);
        authenticationConfigurator = createAuthenticationConfigurator(config, remoteRepoUrl);
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running, even if there are no changes
        return deployment.isRunning();
    }

    protected GitAuthenticationConfigurator createAuthenticationConfigurator(Configuration config,
                                                                             String repoUrl) throws
                                                                                             ConfigurationException {
        return authConfiguratorFactory.forUrl(repoUrl)
                .withUsername(getStringProperty(config, REMOTE_REPO_USERNAME_CONFIG_KEY))
                .withPassword(getStringProperty(config, REMOTE_REPO_PASSWORD_CONFIG_KEY))
                .withPrivateKeyPath(getStringProperty(config, REMOTE_REPO_SSH_PRV_KEY_PATH_CONFIG_KEY))
                .withPrivateKeyPassphrase(getStringProperty(config, REMOTE_REPO_SSH_PRV_KEY_PASSPHRASE_CONFIG_KEY))
                .build();
    }

    protected Git openLocalRepository() throws DeployerException {
        try {
            logger.debug("Opening local Git repository at {}", localRepoFolder);

            return GitUtils.openRepository(localRepoFolder);
        } catch (IOException e) {
            throw new DeployerException("Failed to open Git repository at " + localRepoFolder, e);
        }
    }

}
