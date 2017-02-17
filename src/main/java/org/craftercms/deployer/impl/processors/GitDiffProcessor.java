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
import java.util.Objects;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.GitUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSED_COMMIT_FILE_EXTENSION;

/**
 * Created by alfonso on 2/17/17.
 */
public class GitDiffProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GitPullProcessor.class);

    protected File localRepoFolder;
    protected File processedCommitsFolder;

    @Required
    public void setLocalRepoFolder(File localRepoFolder) {
        this.localRepoFolder = localRepoFolder;
    }

    @Required
    public void setProcessedCommitsFolder(File processedCommitsFolder) {
        this.processedCommitsFolder = processedCommitsFolder;
    }

    @Override
    protected void doConfigure(Configuration config) throws DeployerException {
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running
        return deployment.isRunning();
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
        try (Git git = openLocalRepository()) {
            ObjectId previousCommitId = loadProcessedCommitId();
            ObjectId latestCommitId = getLatestCommitId(git);
            ChangeSet changeSet = resolveChangeSetFromCommits(git, previousCommitId, latestCommitId);

            if (changeSet != null) {
                execution.setStatusDetails("Changes detected and resolved succesfully");
            } else {
                execution.setStatusDetails("No changes detected");
            }

            storeProcessedCommitId(latestCommitId);

            return changeSet;
        }
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

    protected Git openLocalRepository() throws DeployerException {
        try {
            logger.debug("Opening local Git repository at {}", localRepoFolder);

            return GitUtils.openRepository(localRepoFolder);
        } catch (IOException e) {
            throw new DeployerException("Failed to open Git repository at " + localRepoFolder, e);
        }
    }

    protected ObjectId loadProcessedCommitId() throws DeployerException {
        File commitFile = new File(processedCommitsFolder, targetId + "." + PROCESSED_COMMIT_FILE_EXTENSION);
        try {
            if (commitFile.exists()) {
                String commitId = FileUtils.readFileToString(commitFile, "UTF-8").trim();

                logger.info("Found previous processed commit ID for target '{}': {}", targetId, commitId);

                return ObjectId.fromString(commitId);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new DeployerException("Error retrieving previous processed commit ID from " + commitFile, e);
        }
    }

    protected void storeProcessedCommitId(ObjectId commitId) throws DeployerException {
        File commitFile = new File(processedCommitsFolder, targetId + "." + PROCESSED_COMMIT_FILE_EXTENSION);
        try {
            logger.info("Storing processed commit ID {} for target '{}'", commitId.name(), targetId);

            FileUtils.write(commitFile, commitId.name(), "UTF-8", false);
        } catch (IOException e) {
            throw new DeployerException("Error saving processed commit ID to " + commitFile, e);
        }
    }

    protected ObjectId getLatestCommitId(Git git) throws DeployerException {
        try {
            return git.getRepository().resolve(Constants.HEAD);
        } catch (IOException e) {
            throw new DeployerException("Unable to retrieve HEAD commit ID", e);
        }
    }

    protected ChangeSet resolveChangeSetFromCommits(Git git, ObjectId fromCommitId, ObjectId toCommitId) throws DeployerException {
        String fromCommitIdStr = fromCommitId != null? fromCommitId.name(): "{empty}";
        String toCommitIdStr = toCommitId != null? toCommitId.name(): "{empty}";

        if (!Objects.equals(fromCommitId, toCommitId)) {
            logger.info("Calculating change set from commits: {} -> {}", fromCommitIdStr, toCommitIdStr);

            try (ObjectReader reader = git.getRepository().newObjectReader()) {
                AbstractTreeIterator fromTreeIter = getTreeIteratorForCommit(git, reader, fromCommitId);
                AbstractTreeIterator toTreeIter = getTreeIteratorForCommit(git, reader, toCommitId);

                List<DiffEntry> diffEntries = git.diff().setOldTree(fromTreeIter).setNewTree(toTreeIter).call();

                return processDiffEntries(diffEntries);
            } catch (IOException | GitAPIException e) {
                throw new DeployerException("Failed to calculate change set from commits: " + fromCommitIdStr + " -> " + toCommitIdStr, e);
            }
        } else {
            logger.info("Both {} and {} commits are the same. No change set will be calculated", fromCommitIdStr, toCommitIdStr);

            return null;
        }
    }

    protected AbstractTreeIterator getTreeIteratorForCommit(Git git, ObjectReader reader, ObjectId commitId) throws IOException {
        if (commitId != null) {
            RevTree tree = getTreeForCommit(git.getRepository(), commitId);
            CanonicalTreeParser treeParser = new CanonicalTreeParser();

            treeParser.reset(reader, tree.getId());

            return treeParser;
        } else {
            return new EmptyTreeIterator();
        }
    }

    protected RevTree getTreeForCommit(Repository repo, ObjectId commitId) throws IOException  {
        try (RevWalk revWalk = new RevWalk(repo)) {
            RevCommit commit = revWalk.parseCommit(commitId);

            return commit.getTree();
        }
    }

    protected ChangeSet processDiffEntries(List<DiffEntry> diffEntries) {
        List<String> createdFiles = new ArrayList<>();
        List<String> updatedFiles = new ArrayList<>();
        List<String> deletedFiles = new ArrayList<>();

        for (DiffEntry entry : diffEntries) {
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

        return new ChangeSet(createdFiles, updatedFiles, deletedFiles);
    }

}
