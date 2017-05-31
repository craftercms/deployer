/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
package org.craftercms.deployer.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.utils.git.GitAuthenticationConfigurator;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;

/**
 * Utility methods for Git operations.
 *
 * @author avasquez
 */
public abstract class GitUtils {

    public static final String CORE_CONFIG_SECTION = "core";
    public static final String BIG_FILE_THRESHOLD_CONFIG_PARAM = "bigFileThreshold";
    public static final String COMPRESSION_CONFIG_PARAM = "compression";
    public static final String FILE_MODE_CONFIG_PARAM = "fileMode";

    public static final String BIG_FILE_THRESHOLD_DEFAULT = "20m";
    public static final int COMPRESSION_DEFAULT = 0;
    public static final boolean FILE_MODE_DEFAULT = false;

    private GitUtils() {
    }

    /**
     * Opens the Git repository at the specified location.
     *
     * @param localRepositoryFolder the folder where the Git repository is
     *
     * @return the Git instance used to handle the repository
     *
     * @throws IOException if an error occurs
     */
    public static Git openRepository(File localRepositoryFolder) throws IOException {
        return Git.open(localRepositoryFolder);
    }

    /**
     * Clones a remote repository into a specific local folder.
     *
     * @param remoteRepoUrl     the URL of the remote repository. This should be a legal Git URL.
     * @param branch            the branch which should be cloned
     * @param authConfigurator  the {@link GitAuthenticationConfigurator} class used to configure the authentication with the remote
     *                          repository
     * @param localFolder       the local folder into which the remote repository should be cloned
     * @param bigFileThreshold  the value of the Git {@code core.bigFileThreshold} config property
     * @param compression       the value of the Git {@code core.compression} config property
     * @param fileMode          the value of the Git {@code core.fileMode} config property
     *
     * @return the Git instance used to handle the cloned repository
     *
     * @throws GitAPIException  if a Git related error occurs
     * @throws IOException      if an IO error occurs
     */
    public static Git cloneRemoteRepository(String remoteRepoUrl, String branch, GitAuthenticationConfigurator authConfigurator,
                                            File localFolder, String bigFileThreshold, Integer compression, Boolean fileMode)
        throws GitAPIException, IOException{
        CloneCommand command = Git.cloneRepository();
        command.setURI(remoteRepoUrl);
        command.setDirectory(localFolder);

        if (StringUtils.isNotEmpty(branch)) {
            command.setBranch(branch);
        }

        if (authConfigurator != null) {
            authConfigurator.configureAuthentication(command);
        }

        Git git = command.call();
        StoredConfig config = git.getRepository().getConfig();

        if (StringUtils.isEmpty(bigFileThreshold)) {
            bigFileThreshold = BIG_FILE_THRESHOLD_DEFAULT;
        }
        if (compression == null) {
            compression = COMPRESSION_DEFAULT;
        }
        if (fileMode == null) {
            fileMode = FILE_MODE_DEFAULT;
        }

        config.setString(CORE_CONFIG_SECTION, null, BIG_FILE_THRESHOLD_CONFIG_PARAM, bigFileThreshold);
        config.setInt(CORE_CONFIG_SECTION, null, COMPRESSION_CONFIG_PARAM, compression);
        config.setBoolean(CORE_CONFIG_SECTION, null, FILE_MODE_CONFIG_PARAM, fileMode);
        config.save();

        return git;
    }

    /**
     * Execute a Git pull.
     *
     * @param git               the Git instance used to handle the repository
     * @param authConfigurator  the {@link GitAuthenticationConfigurator} class used to configure the authentication with the remote
     *                          repository
     * @param useRebase         if rebase should be used instead of merge after fetching
     *
     * @return the result of the pull
     *
     * @throws GitAPIException if a Git related error occurs
     */
    public static PullResult pull(Git git, GitAuthenticationConfigurator authConfigurator, boolean useRebase) throws GitAPIException {
        PullCommand command = git.pull();

        if (authConfigurator != null) {
            authConfigurator.configureAuthentication(command);
        }

        command.setRebase(useRebase);

        return command.call();
    }

}
