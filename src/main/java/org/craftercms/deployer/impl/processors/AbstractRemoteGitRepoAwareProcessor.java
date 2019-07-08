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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.git.auth.BasicUsernamePasswordAuthConfigurator;
import org.craftercms.commons.git.auth.GitAuthenticationConfigurator;
import org.craftercms.commons.git.auth.SshRsaKeyPairAuthConfigurator;
import org.craftercms.commons.git.auth.SshUsernamePasswordAuthConfigurator;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;

import static org.craftercms.deployer.utils.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.utils.ConfigUtils.getStringProperty;

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

    protected static final String GIT_SSH_URL_REGEX = "^(ssh://.+)|([a-zA-Z0-9._-]+@.+)$";

    private static final Logger logger = LoggerFactory.getLogger(GitPullProcessor.class);

    protected File localRepoFolder;

    // Config properties (populated on init)

    protected String remoteRepoUrl;
    protected String remoteRepoBranch;
    protected GitAuthenticationConfigurator authenticationConfigurator;

    @Required
    public void setLocalRepoFolder(File localRepoFolder) {
        this.localRepoFolder = localRepoFolder;
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
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running, even if there are no changes
        return deployment.isRunning();
    }

    protected GitAuthenticationConfigurator createAuthenticationConfigurator(Configuration config,
                                                                             String repoUrl) throws
                                                                                             ConfigurationException {
        GitAuthenticationConfigurator authConfigurator = null;

        if (repoUrl.matches(GIT_SSH_URL_REGEX)) {
            String password = getStringProperty(config, REMOTE_REPO_PASSWORD_CONFIG_KEY);

            if (StringUtils.isNotEmpty(password)) {
                logger.debug("SSH username/password authentication will be used to connect to repo {}", repoUrl);

                authConfigurator = new SshUsernamePasswordAuthConfigurator(password);
            } else {
                String privateKeyPath = getStringProperty(config, REMOTE_REPO_SSH_PRV_KEY_PATH_CONFIG_KEY);
                String passphrase = getStringProperty(config, REMOTE_REPO_SSH_PRV_KEY_PASSPHRASE_CONFIG_KEY);

                logger.debug("SSH RSA key pair authentication will be used to connect to repo {}", repoUrl);

                SshRsaKeyPairAuthConfigurator keyPairAuthConfigurator = new SshRsaKeyPairAuthConfigurator();
                keyPairAuthConfigurator.setPrivateKeyPath(privateKeyPath);
                keyPairAuthConfigurator.setPassphrase(passphrase);

                authConfigurator = keyPairAuthConfigurator;
            }
        } else {
            String username = getStringProperty(config, REMOTE_REPO_USERNAME_CONFIG_KEY);
            String password = getStringProperty(config, REMOTE_REPO_PASSWORD_CONFIG_KEY);

            if (StringUtils.isNotEmpty(username) && StringUtils.isNotEmpty(password)) {
                logger.debug("Username/password authentication will be used to connect to repo {}", repoUrl);

                authConfigurator = new BasicUsernamePasswordAuthConfigurator(username, password);
            }
        }

        return authConfigurator;
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
