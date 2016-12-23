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
package org.craftercms.deployer.impl.processors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.impl.ChangeSetImpl;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.craftercms.deployer.utils.GitUtils;
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

/**
 * Created by alfonsovasquez on 1/12/16.
 */
public class GitPullProcessor implements DeploymentProcessor {

    public static final String LOCAL_REPOSITORY_PATH_PROPERTY = "localRepository.path";
    public static final String REMOTE_REPOSITORY_URL_PROPERTY = "remoteRepository.url";
    public static final String REMOTE_REPOSITORY_USERNAME_PROPERTY = "remoteRepository.username";
    public static final String REMOTE_REPOSITORY_PASSWORD_PROPERTY = "remoteRepository.password";

    public static final String GIT_FOLDER_NAME = ".git";

    private static final Logger logger = LoggerFactory.getLogger(GitPullProcessor.class);

    protected File localRepositoryFolder;
    protected String remoteRepositoryUrl;
    protected String remoteRepositoryUsername;
    protected String remoteRepositoryPassword;
    protected Git git;

    @Override
    public void init(Configuration configuration) throws DeploymentException {
        initProperties(configuration);

        File gitFolder = new File(localRepositoryFolder, GIT_FOLDER_NAME);

        if (localRepositoryFolder.exists() && gitFolder.exists()) {
            openLocalRepository();
        } else {
            cloneRemoteRepository();
        }
    }

    @Override
    public void destroy() {
        git.close();
    }

    @Override
    public ChangeSet execute(DeploymentContext context, ChangeSet changeSet) throws DeploymentException {
        changeSet = null;

        logger.info("Executing git pull for repository {}", localRepositoryFolder);

        try {
            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            PullResult pullResult = git.pull().call();

            if (pullResult.isSuccessful()) {
                MergeResult mergeResult = pullResult.getMergeResult();
                switch (mergeResult.getMergeStatus()) {
                    case FAST_FORWARD:
                        logger.info("Changes successfully pulled from remote {} for repository {}. Processing them...",
                                    remoteRepositoryUrl, localRepositoryFolder);
                        changeSet = processPull(head, mergeResult.getNewHead());
                        break;
                    case ALREADY_UP_TO_DATE:
                        logger.info("Git repository {} up to date (no changes pulled from remote {})",
                                    localRepositoryFolder, remoteRepositoryUrl);
                        break;
                    default:
                        // Not supported merge results
                        throw new DeploymentException("Received unsupported merge result after executing pull command: " +
                                                      mergeResult.getMergeStatus());
                }
            }
        } catch (Exception e) {
            throw new DeploymentException("Git pull for repository " + localRepositoryFolder + " failed", e);
        }

        logger.info("Git pull for repository {} finished", localRepositoryFolder);

        return changeSet;
    }

    protected void initProperties(Configuration configuration) {
        localRepositoryFolder = new File(ConfigurationUtils.getString(configuration, LOCAL_REPOSITORY_PATH_PROPERTY, true));
        remoteRepositoryUrl = ConfigurationUtils.getString(configuration, REMOTE_REPOSITORY_URL_PROPERTY, true);
        remoteRepositoryUsername = ConfigurationUtils.getString(configuration, REMOTE_REPOSITORY_USERNAME_PROPERTY, false);
        remoteRepositoryPassword = ConfigurationUtils.getString(configuration, REMOTE_REPOSITORY_PASSWORD_PROPERTY, false);
    }

    protected void openLocalRepository() {
        try {
            logger.info("Opening local Git repository at {}", localRepositoryFolder);

            git = GitUtils.openRepository(localRepositoryFolder);
        } catch (IOException e) {
            throw new DeploymentException("Failed to open Git repository at " + localRepositoryFolder, e);
        }
    }

    protected void cloneRemoteRepository() {
        try {
            if (localRepositoryFolder.exists()) {
                logger.debug("Deleting existing folder '{}' before cloning", localRepositoryFolder);

                FileUtils.forceDelete(localRepositoryFolder);
            } else {
                logger.debug("Creating folder '{}' and any nonexistent parents before cloning", localRepositoryFolder);

                FileUtils.forceMkdir(localRepositoryFolder);
            }

            logger.info("Cloning Git repository from {} to {}", remoteRepositoryUrl, localRepositoryFolder);

            if (StringUtils.isNotEmpty(remoteRepositoryUsername)) {
                git = GitUtils.cloneRemoteRepository(remoteRepositoryUrl, remoteRepositoryUsername, remoteRepositoryPassword,
                                                     localRepositoryFolder);
            } else {
                git = GitUtils.cloneRemoteRepository(remoteRepositoryUrl, localRepositoryFolder);
            }
        } catch (IOException | GitAPIException e) {
            throw new DeploymentException("Failed to clone Git repository from " + remoteRepositoryUrl + " to " + localRepositoryFolder, e);
        }
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
                switch (entry.getChangeType()) {
                    case MODIFY:
                        updatedFiles.add(entry.getNewPath());
                        logger.debug("Updated file: {}", entry.getNewPath());
                        break;
                    case DELETE:
                        deletedFiles.add(entry.getOldPath());
                        logger.debug("Deleted file: {}", entry.getOldPath());
                        break;
                    case RENAME:
                        deletedFiles.add(entry.getOldPath());
                        createdFiles.add(entry.getNewPath());
                        logger.debug("Renamed file: {} -> {}", entry.getOldPath(), entry.getNewPath());
                        break;
                    case COPY:
                        createdFiles.add(entry.getNewPath());
                        logger.debug("Copied file: {} -> {}", entry.getOldPath(), entry.getNewPath());
                        break;
                    default: // ADD
                        createdFiles.add(entry.getNewPath());
                        logger.debug("Created file: {}", entry.getNewPath());
                        break;
                }
            }
        }

        return new ChangeSetImpl(createdFiles, updatedFiles, deletedFiles);
    }

}
