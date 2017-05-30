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
package org.craftercms.deployer.impl.processors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.ProcessedCommitsStore;
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

import static org.craftercms.deployer.impl.DeploymentConstants.REPROCESS_ALL_FILES_PARAM_NAME;

/**
 * Processor that, based on a previous processed commit that's stored, does a diff with the current commit of the deployment, to
 * find out the change set. If there is no previous processed commit, then the entire repository becomes the change set. This processor
 * is used basically to create the change set and should be used before other processors that actually process the change set, like
 * {@link SearchIndexingProcessor}.
 *
 * @author avasquez
 */
public class GitDiffProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(GitDiffProcessor.class);

    protected File localRepoFolder;
    protected ProcessedCommitsStore processedCommitsStore;

    /**
     * Sets the local filesystem folder the contains the deployed repository.
     */
    @Required
    public void setLocalRepoFolder(File localRepoFolder) {
        this.localRepoFolder = localRepoFolder;
    }

    /**
     * Sets the store for processed commits.
     */
    @Required
    public void setProcessedCommitsStore(ProcessedCommitsStore processedCommitsStore) {
        this.processedCommitsStore = processedCommitsStore;
    }

    @Override
    protected void doInit(Configuration config) throws DeployerException {
    }

    @Override
    public void destroy() throws DeployerException {
    }

    @Override
    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running
        return deployment.isRunning();
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet, Map<String, Object> params) throws DeployerException {
        boolean reprocessAllFiles = getReprocessAllFilesParam(params);
        if (reprocessAllFiles) {
            processedCommitsStore.delete(targetId);

            logger.info("All files from local repo {} will be reprocessed", localRepoFolder);
        }

        try (Git git = openLocalRepository()) {
            ObjectId previousCommitId = processedCommitsStore.load(targetId);
            ObjectId latestCommitId = getLatestCommitId(git);
            ChangeSet changeSet = resolveChangeSetFromCommits(git, previousCommitId, latestCommitId);

            if (changeSet != null) {
                execution.setStatusDetails("Changes detected and resolved succesfully");
            } else {
                execution.setStatusDetails("No changes detected");
            }

            processedCommitsStore.store(targetId, latestCommitId);

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
        String newPath;
        String oldPath;

        for (DiffEntry entry : diffEntries) {
            switch (entry.getChangeType()) {
                case MODIFY:
                    newPath = asContentStoreUrl(entry.getNewPath());

                    updatedFiles.add(newPath);

                    logger.debug("Updated file: {}", newPath);
                    break;
                case DELETE:
                    oldPath = asContentStoreUrl(entry.getOldPath());

                    deletedFiles.add(oldPath);

                    logger.debug("Deleted file: {}", oldPath);
                    break;
                case RENAME:
                    oldPath = asContentStoreUrl(entry.getOldPath());
                    newPath = asContentStoreUrl(entry.getNewPath());

                    deletedFiles.add(oldPath);
                    createdFiles.add(newPath);

                    logger.debug("Renamed file: {} -> {}", oldPath, newPath);
                    break;
                case COPY:
                    oldPath = asContentStoreUrl(entry.getOldPath());
                    newPath = asContentStoreUrl(entry.getNewPath());

                    createdFiles.add(newPath);

                    logger.debug("Copied file: {} -> {}", oldPath, newPath);
                    break;
                default: // ADD
                    newPath = asContentStoreUrl(entry.getNewPath());

                    createdFiles.add(newPath);

                    logger.debug("New file: {}", newPath);
                    break;
            }
        }

        return new ChangeSet(createdFiles, updatedFiles, deletedFiles);
    }

    protected String asContentStoreUrl(String path) {
        path = FilenameUtils.separatorsToUnix(path);

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path;
    }

    protected boolean getReprocessAllFilesParam(Map<String, Object> params) {
        if (MapUtils.isNotEmpty(params)) {
            Object value = params.get(REPROCESS_ALL_FILES_PARAM_NAME);
            if (value instanceof Boolean) {
                return (Boolean)value;
            } else {
                return value != null && BooleanUtils.toBoolean(value.toString());
            }
        } else {
            return false;
        }
    }

}
