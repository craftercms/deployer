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

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
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

    public static Git cloneRemoteRepository(String repositoryUrl, String branch, String username, String password,
                                            File localFolder, String bigFileThreshold, Integer compression)
        throws GitAPIException, IOException {
        CloneCommand command = Git.cloneRepository();
        command.setURI(repositoryUrl);
        command.setDirectory(localFolder);

        if (StringUtils.isNotEmpty(branch)) {
            command.setBranch(branch);
        }
        if (StringUtils.isNotEmpty(username)) {
            command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
        }

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

}
