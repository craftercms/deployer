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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Created by alfonsovasquez on 12/15/16.
 */
public abstract class GitUtils {

    private GitUtils() {
    }

    public static Git openRepository(File localRepositoryFolder) throws IOException {
        return Git.open(localRepositoryFolder);
    }

    public static Git cloneRemoteRepository(String remoteRepositoryUrl, File localRepositoryFolder) throws GitAPIException {
        return Git.cloneRepository()
            .setURI(remoteRepositoryUrl)
            .setDirectory(localRepositoryFolder)
            .call();
    }

    public static Git cloneRemoteRepository(String remoteRepositoryUrl, String remoteRepositoryUsername, String remoteRepositoryPassword,
                                            File localRepositoryFolder)
        throws GitAPIException {
        return Git.cloneRepository()
            .setURI(remoteRepositoryUrl)
            .setDirectory(localRepositoryFolder)
            .setCredentialsProvider(new UsernamePasswordCredentialsProvider(remoteRepositoryUsername, remoteRepositoryPassword))
            .call();
    }

}
