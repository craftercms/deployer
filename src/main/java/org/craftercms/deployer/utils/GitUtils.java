/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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

import com.jcraft.jsch.Session;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Created by alfonsovasquez on 12/15/16.
 */
public abstract class GitUtils {

    public static final String CORE_CONFIG_SECTION = "core";
    public static final String BIG_FILE_THRESHOLD_CONFIG_PARAM = "bigFileThreshold";
    public static final String COMPRESSION_CONFIG_PARAM = "compression";

    public static final String BIG_FILE_THRESHOLD_DEFAULT = "20m";
    public static final int COMPRESSION_DEFAULT = 0;

    private GitUtils() {
    }

    public static Git openRepository(File localRepositoryFolder) throws IOException {
        return Git.open(localRepositoryFolder);
    }

    public static Git cloneRemoteRepository(String remoteRepoUrl, String branch, String username, String password, File localFolder,
                                            String bigFileThreshold, Integer compression)
        throws GitAPIException, IOException, IllegalArgumentException {
        CloneCommand command = Git.cloneRepository();
        command.setURI(remoteRepoUrl);
        command.setDirectory(localFolder);

        if (StringUtils.isNotEmpty(branch)) {
            command.setBranch(branch);
        }

        addAuthentication(command, remoteRepoUrl, username, password);

        Git git = command.call();
        StoredConfig config = git.getRepository().getConfig();

        if (StringUtils.isEmpty(bigFileThreshold)) {
            bigFileThreshold = BIG_FILE_THRESHOLD_DEFAULT;
        }
        if (compression == null) {
            compression = COMPRESSION_DEFAULT;
        }

        config.setString(CORE_CONFIG_SECTION, null, BIG_FILE_THRESHOLD_CONFIG_PARAM, bigFileThreshold);
        config.setInt(CORE_CONFIG_SECTION, null, COMPRESSION_CONFIG_PARAM, compression);
        config.save();

        return git;
    }

    public static PullResult pull(Git git, String remoteRepoUrl, String username, String password) throws GitAPIException {
        PullCommand command = git.pull();
        addAuthentication(command, remoteRepoUrl, username, password);

        return command.call();
    }

    private static void addAuthentication(TransportCommand command, String remoteRepoUrl, String username, String password) {
        if (remoteRepoUrl.startsWith("ssh:")) {
            if (StringUtils.isNotEmpty(password)) {
                SshSessionFactory sshSessionFactory = new SshSessionFactoryWithPassword(password);
                command.setTransportConfigCallback(transport -> ((SshTransport) transport).setSshSessionFactory(sshSessionFactory));
            } else {
                throw new IllegalArgumentException("Repository URL uses the SSH protocol but no password was provided");
            }
        } else if (StringUtils.isNotEmpty(username)) {
            command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        }
    }

    public static final class SshSessionFactoryWithPassword extends JschConfigSessionFactory {

        private String password;

        public SshSessionFactoryWithPassword(String password) {
            this.password = password;
        }

        @Override
        protected void configure(OpenSshConfig.Host hc, Session session) {
            session.setPassword(password);
        }

    }

}
