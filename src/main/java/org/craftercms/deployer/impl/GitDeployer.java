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
package org.craftercms.deployer.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.io.FileUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployer;
import org.craftercms.deployer.api.exception.DeploymentException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 1/12/16.
 */
public class GitDeployer implements Deployer {

    public static final String GIT_FOLDER_NAME = ".git";

    private static final Logger logger = LoggerFactory.getLogger(GitDeployer.class);

    protected String localRepositoryPath;
    protected String remoteRepositoryUrl;
    protected Git git;

    @Required
    public void setLocalRepositoryPath(String localRepositoryPath) {
        this.localRepositoryPath = localRepositoryPath;
    }

    @Required
    public void setRemoteRepositoryUrl(String remoteRepositoryUrl) {
        this.remoteRepositoryUrl = remoteRepositoryUrl;
    }

    @PostConstruct
    public void init() throws IOException, GitAPIException {
        File localRepositoryFolder = new File(localRepositoryPath);
        File gitFolder = new File(localRepositoryFolder, GIT_FOLDER_NAME);

        if (localRepositoryFolder.exists() && gitFolder.exists()) {
            git = openRepository(localRepositoryFolder);
        } else {
            FileUtils.forceDelete(localRepositoryFolder);

            git = cloneRemoteRepository(remoteRepositoryUrl, localRepositoryFolder);
        }
    }

    @PreDestroy
    public void destroy() {
        git.close();
    }

    @Override
    public ChangeSet deploy() throws DeploymentException {
        ChangeSet changeSet = null;

        logger.debug("Executing deployment for Git repository {}", localRepositoryPath);

        try {
            ObjectId head = getRepositoryHead();
            PullResult pullResult = doPull();

            if (pullResult.isSuccessful()) {
                MergeResult mergeResult = pullResult.getMergeResult();
                switch (mergeResult.getMergeStatus()) {
                    case FAST_FORWARD:
                        changeSet = processPull(head, mergeResult.getNewHead());
                        break;
                    case ALREADY_UP_TO_DATE:
                        logger.debug("Git repository {} up to date", localRepositoryPath);
                        break;
                    default:
                        // Not supported merge result
                        throw new DeploymentException("Received unsupported merge result after executing pull command: " +
                                                      mergeResult.getMergeStatus());
                }
            }
        } catch (Exception e) {
            throw new DeploymentException("Deployment for Git repository " + localRepositoryPath + " failed: " + e.getMessage(), e);
        }

        logger.debug("Deployment for Git repository {} finished", localRepositoryPath);

        return changeSet;
    }

    protected Git openRepository(File localRepositoryFolder) throws IOException {
        logger.debug("Opening Git repository at {}", localRepositoryFolder);

        return Git.open(localRepositoryFolder);
    }

    protected Git cloneRemoteRepository(String remoteRepositoryUrl, File localRepositoryFolder) throws GitAPIException {
        logger.debug("Cloning Git repository from {} to {}", remoteRepositoryUrl, localRepositoryFolder);

        return Git.cloneRepository()
            .setURI(remoteRepositoryUrl)
            .setDirectory(localRepositoryFolder)
            .call();
    }

    protected ObjectId getRepositoryHead() throws IOException {
        return git.getRepository().resolve(Constants.HEAD);
    }

    protected PullResult doPull() throws GitAPIException {
        logger.debug("Doing pull for Git repository {}", localRepositoryPath);

        return git.pull().call();
    }

    protected ChangeSet processPull(ObjectId oldHead, ObjectId newHead) throws IOException, GitAPIException {
        List<String> createdFiles = new ArrayList<>();
        List<String> updatedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();
        RevWalk revWalk = new RevWalk(git.getRepository());
        ObjectId oldHeadTree = revWalk.parseCommit(oldHead).getTree().getId();
        ObjectId newHeadTree = revWalk.parseCommit(newHead).getTree().getId();

        // prepare the two iterators to compute the diff between
        try (ObjectReader reader = git.getRepository().newObjectReader()) {
            CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
            CanonicalTreeParser newTreeIter = new CanonicalTreeParser();

            oldTreeIter.reset(reader, oldHeadTree);
            newTreeIter.reset(reader, newHeadTree);

            // finally get the list of changed files
            List<DiffEntry> diffs = git.diff()
                .setNewTree(newTreeIter)
                .setOldTree(oldTreeIter)
                .call();
            for (DiffEntry entry : diffs) {
                logger.debug("Git diff entry: {}", entry);

                switch (entry.getChangeType()) {
                    case MODIFY:
                        updatedFiles.add(entry.getNewPath());
                        break;
                    case DELETE:
                        deletedFiles.add(entry.getOldPath());
                        break;
                    case RENAME:
                        deletedFiles.add(entry.getOldPath());
                        createdFiles.add(entry.getNewPath());
                        break;
                    default: // ADD or COPY
                        createdFiles.add(entry.getNewPath());
                        break;
                }
            }
        }

        return new ChangeSetImpl(createdFiles, updatedFiles, deletedFiles);
    }

}
