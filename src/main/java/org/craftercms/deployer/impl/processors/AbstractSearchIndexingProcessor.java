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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.core.util.cache.CacheTemplate;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static org.craftercms.deployer.utils.ConfigUtils.getBooleanProperty;
import static org.craftercms.deployer.utils.ConfigUtils.getStringProperty;

/**
 * Processor that indexes the files on the change set, using one or several {@link BatchIndexer}. After the files have
 * been indexed it submits a commit. A processor instance can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>ignoreIndexId:</strong> If the index ID should be ignored, in other words, if the index ID should
 *     always be null on update calls.</li>
 *     <li><strong>indexId:</strong> The specific index ID to use</li>
 *     <li><strong>reindexItemsOnComponentUpdates:</strong> Flag that indicates that if a component is updated, all
 *     other pages and components that include it should be updated too. This needs to be done when flattening is
 *     enabled, since the component needs to be re-included in pages/components. By default is true.</li>
 * </ul>
 *
 * @author avasquez
 */
public abstract class AbstractSearchIndexingProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractSearchIndexingProcessor.class);

    protected static final String INDEX_ID_CONFIG_KEY = "indexId";
    protected static final String IGNORE_INDEX_ID_CONFIG_KEY = "ignoreIndexId";
    protected static final String REINDEX_ITEMS_ON_COMPONENT_UPDATES = "reindexItemsOnComponentUpdates";

    protected static final Pattern DEFAULT_DESCRIPTOR_PATH_PATTERN = Pattern.compile("^/site/.+\\.xml$");
    protected static final Pattern DEFAULT_COMPONENT_PATH_PATTERN = Pattern.compile("^/site/components/.+$");
    protected static final int DEFAULT_ITEMS_THAT_INCLUDE_COMPONENT_QUERY_ROWS = 100;

    protected CacheTemplate cacheTemplate;
    protected ObjectFactory<Context> contextFactory;
    protected ContentStoreService contentStoreService;
    protected List<BatchIndexer> batchIndexers;
    protected boolean xmlFlatteningEnabled;
    protected Pattern descriptorPathPattern;
    protected Pattern componentPathPattern;
    protected int itemsThatIncludeComponentQueryRows;
    protected String indexIdFormat;

    // Config properties (populated on init)

    protected String indexId;
    protected boolean reindexItemsOnComponentUpdates;

    public AbstractSearchIndexingProcessor() {
        this.descriptorPathPattern = DEFAULT_DESCRIPTOR_PATH_PATTERN;
        this.componentPathPattern = DEFAULT_COMPONENT_PATH_PATTERN;
        this.itemsThatIncludeComponentQueryRows = DEFAULT_ITEMS_THAT_INCLUDE_COMPONENT_QUERY_ROWS;
    }

    public void setCacheTemplate(final CacheTemplate cacheTemplate) {
        this.cacheTemplate = cacheTemplate;
    }

    /**
     * Sets the factory for the {@link Context}.
     */
    @Required
    public void setContextFactory(ObjectFactory<Context> contextFactory) {
        this.contextFactory = contextFactory;
    }

    /**
     * Sets the content store used to retrieve the files to index.
     */
    @Required
    public void setContentStoreService(ContentStoreService contentStoreService) {
        this.contentStoreService = contentStoreService;
    }

    /**
     * Sets the single batch indexer used for indexing.
     */
    public void setBatchIndexer(BatchIndexer batchIndexer) {
        this.batchIndexers = Collections.singletonList(batchIndexer);
    }

    /**
     * Sets the list of batch indexers used for indexing.
     */
    public void setBatchIndexers(List<BatchIndexer> batchIndexers) {
        this.batchIndexers = batchIndexers;
    }

    /**
     * Sets whether XML flattening is enabled. Only used in conjunction with {@code reindexItemsOnComponentUpdates}
     * to see if pages/components should be re-indexed when components they include are updated.
     */
    public void setXmlFlatteningEnabled(boolean xmlFlatteningEnabled) {
        this.xmlFlatteningEnabled = xmlFlatteningEnabled;
    }

    /**
     * Sets the regex used to match descriptor paths for detecting inheriting items that should be reindex.
     */
    public void setDescriptorPathRegex(String descriptorPathRegex) {
        descriptorPathPattern = Pattern.compile(descriptorPathRegex);
    }

    /**
     * Sets the regex used to match component paths (used when {@code reindexItemsOnComponentUpdates} is enabled).
     */
    public void setComponentPathRegex(String componentPathRegex) {
        componentPathPattern = Pattern.compile(componentPathRegex);
    }

    /**
     * Sets the rows to fetch for the search query used to find items that include components (used when
     * {@code reindexItemsOnComponentUpdates} is enabled).
     */
    public void setItemsThatIncludeComponentQueryRows(int itemsThatIncludeComponentQueryRows) {
        this.itemsThatIncludeComponentQueryRows = itemsThatIncludeComponentQueryRows;
    }

    /**
     * The format used for the index id
     */
    @Required
    public void setIndexIdFormat(String indexIdFormat) {
        this.indexIdFormat = indexIdFormat;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        boolean ignoreIndexId = getBooleanProperty(config, IGNORE_INDEX_ID_CONFIG_KEY, false);
        if (ignoreIndexId) {
            indexId = null;
        } else {
            indexId = getStringProperty(config, INDEX_ID_CONFIG_KEY);
            if (StringUtils.isEmpty(indexId)) {
                indexId = String.format(indexIdFormat, siteName);
            }
        }

        reindexItemsOnComponentUpdates = getBooleanProperty(config, REINDEX_ITEMS_ON_COMPONENT_UPDATES, true);

        if (CollectionUtils.isEmpty(batchIndexers)) {
            throw new IllegalStateException("At least one batch indexer should be provided");
        }
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    /**
     * Override to add pages/components that need to be updated because a component that they include was updated.
     *
     * @param changeSet original change set
     * @return filtered change set
     */
    @Override
    protected ChangeSet getFilteredChangeSet(ChangeSet changeSet) {
        changeSet = super.getFilteredChangeSet(changeSet);
        if (changeSet != null && !changeSet.isEmpty() && xmlFlatteningEnabled) {
            List<String> createdFiles = changeSet.getCreatedFiles();
            List<String> updatedFiles = changeSet.getUpdatedFiles();
            List<String> deletedFiles = changeSet.getDeletedFiles();
            List<String> newUpdatedFiles = new ArrayList<>(updatedFiles);

            if (CollectionUtils.isNotEmpty(createdFiles)) {
                for (String path : createdFiles) {
                    if (isDescriptor(path)) {
                        addItemsThatInheritFromDescriptorToUpdatedFiles(path, createdFiles, newUpdatedFiles,
                                                                        deletedFiles);
                    }
                    if (reindexItemsOnComponentUpdates && isComponent(path)) {
                        addItemsThatIncludeComponentToUpdatedFiles(path, createdFiles, newUpdatedFiles, deletedFiles);
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                for (String path : updatedFiles) {
                    if (isDescriptor(path)) {
                        addItemsThatInheritFromDescriptorToUpdatedFiles(path, createdFiles, newUpdatedFiles,
                                                                        deletedFiles);
                    }
                    if (reindexItemsOnComponentUpdates && isComponent(path)) {
                        addItemsThatIncludeComponentToUpdatedFiles(path, createdFiles, newUpdatedFiles, deletedFiles);
                    }
                }
            }


            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                for (String path : deletedFiles) {
                    if (isDescriptor(path)) {
                        addItemsThatInheritFromDescriptorToUpdatedFiles(path, createdFiles, newUpdatedFiles,
                                                                        deletedFiles);
                    }
                    if (reindexItemsOnComponentUpdates && isComponent(path)) {
                        addItemsThatIncludeComponentToUpdatedFiles(path, createdFiles, newUpdatedFiles, deletedFiles);
                    }
                }
            }

            ChangeSet filteredChangeSet = new ChangeSet(createdFiles, newUpdatedFiles, deletedFiles);
            filteredChangeSet.setUpdateDetails(changeSet.getUpdateDetails());
            filteredChangeSet.setUpdateLog(changeSet.getUpdateLog());
            return filteredChangeSet;
        } else {
            return changeSet;
        }
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        logger.info("Performing search indexing...");

        List<String> createdFiles = emptyIfNull(filteredChangeSet.getCreatedFiles());
        List<String> updatedFiles = emptyIfNull(filteredChangeSet.getUpdatedFiles());
        List<String> deletedFiles = emptyIfNull(filteredChangeSet.getDeletedFiles());
        UpdateSet updateSet = new UpdateSet(ListUtils.union(createdFiles, updatedFiles), deletedFiles);
        updateSet.setUpdateDetails(filteredChangeSet.getUpdateDetails());
        updateSet.setUpdateLog(filteredChangeSet.getUpdateLog());
        UpdateStatus updateStatus = new UpdateStatus();

        execution.setStatusDetails(updateStatus);

        Context context = contextFactory.getObject();

        logger.debug("Clearing cache for context {}", context);
        cacheTemplate.getCacheService().clearScope(context);

        try {
            for (BatchIndexer indexer : batchIndexers) {
                indexer.updateIndex(indexId, siteName, contentStoreService, context, updateSet,
                                    updateStatus);

                if (updateStatus.getAttemptedUpdatesAndDeletes() > 0) {
                    doCommit(indexId);
                }
            }
        } catch (Exception e) {
            throw new DeployerException("Error while performing search indexing", e);
        }

        return null;
    }

    protected abstract void doCommit(final String indexId);

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    protected boolean isDescriptor(String path) {
        return descriptorPathPattern.matcher(path).matches();
    }

    protected boolean isComponent(String path) {
        return componentPathPattern.matcher(path).matches();
    }

    protected boolean isBeingUpdatedOrDeleted(String path, List<String> createdFiles, List<String> updatedFiles,
                                              List<String> deletedFiles) {
        return createdFiles.contains(path) || updatedFiles.contains(path) || deletedFiles.contains(path);
    }

    protected abstract List<String> getItemsThatInheritDescriptor(String indexId, String descriptorPath);

    protected void addItemsThatInheritFromDescriptorToUpdatedFiles(String descriptorPath, List<String> createdFiles,
                                                                  List<String> updatedFiles,
                                                                   List<String> deletedFiles) {
        addAffectedItemsToUpdatedFiles(descriptorPath, createdFiles, updatedFiles, deletedFiles,
                                        this::getItemsThatInheritDescriptor);
    }

    protected abstract List<String> getItemsThatIncludeComponent(String indexId, String componentPath);

    protected void addItemsThatIncludeComponentToUpdatedFiles(String componentPath, List<String> createdFiles,
                                                              List<String> updatedFiles, List<String> deletedFiles) {
        addAffectedItemsToUpdatedFiles(componentPath, createdFiles, updatedFiles, deletedFiles,
                                        this::getItemsThatIncludeComponent);
    }

    protected void addAffectedItemsToUpdatedFiles(String path, List<String> createdFiles, List<String> updatedFiles,
                                                  List<String> deletedFiles,
                                                  BiFunction<String, String, List<String>> function) {
        List<String> itemPaths = function.apply(indexId, path);
        if (CollectionUtils.isNotEmpty(itemPaths)) {
            for (String itemPath : itemPaths) {
                if (!isBeingUpdatedOrDeleted(itemPath, createdFiles, updatedFiles, deletedFiles)) {
                    logger.debug("Item {} is affected by the update of {}. Adding it to list of updated files.",
                        itemPath, path);

                    updatedFiles.add(itemPath);
                }
            }
        }
    }

}
