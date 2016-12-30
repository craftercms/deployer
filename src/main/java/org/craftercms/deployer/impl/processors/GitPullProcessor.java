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
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.HiddenFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.TargetContext;
import org.craftercms.deployer.api.exceptions.DeploymentException;
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

import static org.craftercms.deployer.impl.CommonConfigurationProperties.TARGET_ROOT_FOLDER_PROPERTY_NAME;

/**
 * Created by alfonsovasquez on 1/12/16.
 */
public class GitPullProcessor extends AbstractDeploymentProcessor {

    public static final String REMOTE_REPOSITORY_URL_PROPERTY_NAME = "remoteRepository.url";
    public static final String REMOTE_REPOSITORY_USERNAME_PROPERTY_NAME = "remoteRepository.username";
    public static final String REMOTE_REPOSITORY_PASSWORD_PROPERTY_NAME = "remoteRepository.password";

    public static final String GIT_FOLDER_NAME = ".git";

    private static final Logger logger = LoggerFactory.getLogger(GitPullProcessor.class);

    protected File localRepositoryFolder;
    protected String remoteRepositoryUrl;
    protected String remoteRepositoryUsername;
    protected String remoteRepositoryPassword;

    @Override
    public void doInit(Configuration mainConfig, Configuration processorConfig) throws DeploymentException {
        // Force, we always want to execute on empty change set
        executeOnEmptyChangeSet = true;
        localRepositoryFolder = new File(ConfigurationUtils.getRequiredString(mainConfig, TARGET_ROOT_FOLDER_PROPERTY_NAME));
        remoteRepositoryUrl = ConfigurationUtils.getRequiredString(processorConfig, REMOTE_REPOSITORY_URL_PROPERTY_NAME);
        remoteRepositoryUsername = ConfigurationUtils.getString(processorConfig, REMOTE_REPOSITORY_USERNAME_PROPERTY_NAME);
        remoteRepositoryPassword = ConfigurationUtils.getString(processorConfig, REMOTE_REPOSITORY_PASSWORD_PROPERTY_NAME);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doExecute(Deployment deployment, ProcessorExecution execution, TargetContext context) throws DeploymentException {
        File gitFolder = new File(localRepositoryFolder, GIT_FOLDER_NAME);
        if (localRepositoryFolder.exists() && gitFolder.exists()) {
            doPull(deployment, execution);
        } else {
            doClone(deployment, execution);
        }
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

    protected void doPull(Deployment deployment, ProcessorExecution execution) throws DeploymentException {
        Git git = openLocalRepository();
        try {
            pullChanges(git, deployment, execution);
        } finally {
            if (git != null) {
                git.close();
            }
        }
    }

    protected Git openLocalRepository() throws DeploymentException {
        try {
            logger.info("Opening local Git repository at {}", localRepositoryFolder);

            return GitUtils.openRepository(localRepositoryFolder);
        } catch (IOException e) {
            throw new DeploymentException("Failed to open Git repository at " + localRepositoryFolder, e);
        }
    }

    protected void pullChanges(Git git, Deployment deployment, ProcessorExecution execution) throws DeploymentException {
        try {
            logger.info("Executing git pull for repository {}...", localRepositoryFolder);

            ObjectId head = git.getRepository().resolve(Constants.HEAD);
            PullResult pullResult = git.pull().call();

            if (pullResult.isSuccessful()) {
                MergeResult mergeResult = pullResult.getMergeResult();
                ChangeSet changeSet;
                String details;

                switch (mergeResult.getMergeStatus()) {
                    case FAST_FORWARD:
                        details = "Changes successfully pulled from remote " + remoteRepositoryUrl + " for repository " +
                                  localRepositoryFolder;

                        logger.info(details);

                        changeSet = resolveChangeSetFromPull(git, head, mergeResult.getNewHead());

                        deployment.setChangeSet(changeSet);
                        execution.setStatusDetails(details);
                        break;
                    case ALREADY_UP_TO_DATE:
                        details = "Git repository " + localRepositoryFolder + " up to date (no changes pulled from remote " +
                                  remoteRepositoryUrl + ")";

                        logger.info(details);

                        execution.setStatusDetails(details);
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
    }

    protected void doClone(Deployment deployment, ProcessorExecution execution) throws DeploymentException {
        Git git = cloneRemoteRepository();
        try {
            resolveChangesFromClone(deployment, execution);
        } finally {
            git.close();
        }
    }

    protected Git cloneRemoteRepository() throws DeploymentException {
        try {
            if (localRepositoryFolder.exists()) {
                logger.debug("Deleting existing folder '{}' before cloning", localRepositoryFolder);

                FileUtils.forceDelete(localRepositoryFolder);
            } else {
                logger.debug("Creating folder '{}' and any nonexistent parents before cloning", localRepositoryFolder);

                FileUtils.forceMkdir(localRepositoryFolder);
            }

            logger.info("Cloning Git remote repository {} into {}", remoteRepositoryUrl, localRepositoryFolder);

            if (StringUtils.isNotEmpty(remoteRepositoryUsername)) {
                return GitUtils.cloneRemoteRepository(remoteRepositoryUrl, remoteRepositoryUsername, remoteRepositoryPassword,
                                                      localRepositoryFolder);
            } else {
                return GitUtils.cloneRemoteRepository(remoteRepositoryUrl, localRepositoryFolder);
            }
        } catch (IOException | GitAPIException e) {
            throw new DeploymentException("Failed to clone Git remote repository " + remoteRepositoryUrl + " into " +
                                          localRepositoryFolder, e);
        }
    }

    protected void resolveChangesFromClone(Deployment deployment, ProcessorExecution execution) {
        logger.info("Adding entire cloned repository to change set...");

        List<String> createdFiles = new ArrayList<>();
        addClonedFilesToChangeSet(localRepositoryFolder, "", createdFiles);

        deployment.setChangeSet(new ChangeSet(createdFiles, Collections.emptyList(), Collections.emptyList()));
        execution.setStatusDetails("Successfully cloned Git remote repository " + remoteRepositoryUrl + " into " + localRepositoryFolder);
    }

    protected void addClonedFilesToChangeSet(File parent, String parentPath, List<String> createdFiles) {
        String[] filenames = parent.list(HiddenFileFilter.VISIBLE);
        if (filenames != null) {
            for (String filename : filenames) {
                File file = new File(parent, filename);
                String path = FilenameUtils.concat(parentPath, filename);

                if (file.isDirectory()) {
                    addClonedFilesToChangeSet(file, path, createdFiles);
                } else {
                    logger.debug("New file: {}", path);

                    createdFiles.add(path);
                }
            }
        }
    }

    protected ChangeSet resolveChangeSetFromPull(Git git, ObjectId oldHead, ObjectId newHead) throws IOException, GitAPIException {
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
                        logger.debug("New file: {}", entry.getNewPath());
                        break;
                }
            }
        }

        return new ChangeSet(createdFiles, updatedFiles, deletedFiles);
    }

}
