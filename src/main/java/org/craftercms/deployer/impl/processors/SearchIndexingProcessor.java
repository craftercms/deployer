package org.craftercms.deployer.impl.processors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.store.impl.filesystem.FileSystemContentStoreAdapter;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.UpdateSet;
import org.craftercms.search.batch.UpdateStatus;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Processor that indexes the files on the change set, using one or several {@link BatchIndexer}. After the files have been indexed it
 * submits a commit. A processor instance can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>ignoreIndexId:</strong> If the index ID should be ignored, in other words, if the index ID should always be null
 *     on update calls.</li>
 *     <li><strong>indexId:</strong> The specific index ID to use</li>
 *     <li><strong>indexIdFormat:</strong> The String.format, based onf the site name, that should be used to generate the index ID.
 *     E.g. a <emp>%s-default'</emp> format with a <em>mysite</em> site name will generate a <em>mysite-default</em> index ID.</li>
 *     <
 * </ul>
 *
 * @author avasquez
 */
public class SearchIndexingProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingProcessor.class);

    public static final String DEFAULT_INDEX_ID_FORMAT = "%s";

    public static final String INDEX_ID_CONFIG_KEY = "indexId";
    public static final String INDEX_ID_FORMAT_CONFIG_KEY = "indexIdFormat";
    public static final String IGNORE_INDEX_ID_CONFIG_KEY = "ignoreIndexId";

    protected String localRepoUrl;
    protected ContentStoreService contentStoreService;
    protected SearchService searchService;
    protected List<BatchIndexer> batchIndexers;
    protected boolean xmlMergingEnabled;
    protected String indexId;
    protected Context context;

    /**
     * Sets the URL of the local repository that will be passed to the {@link ContentStoreService} to retrieve the files to
     * index.
     */
    @Required
    public void setLocalRepoUrl(String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    /**
     * Sets the content store used to retrieve the files to index.
     */
    @Required
    public void setContentStoreService(ContentStoreService contentStoreService) {
        this.contentStoreService = contentStoreService;
    }

    /**
     * Sets the search service. Since all indexing is done through the {@link BatchIndexer}s the search service is only used
     * to commit.
     */
    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
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
     * Sets whether XML merging (aka inheritance) should be enabled when retrieving XML from the {@link ContentStoreService}.
     */
    public void setXmlMergingEnabled(boolean xmlMergingEnabled) {
        this.xmlMergingEnabled = xmlMergingEnabled;
    }

    @Override
    protected void doInit(Configuration config) throws DeployerException {
        boolean ignoreIndexId = ConfigUtils.getBooleanProperty(config, IGNORE_INDEX_ID_CONFIG_KEY, false);
        if (ignoreIndexId) {
            indexId = null;
        } else {
            indexId = ConfigUtils.getStringProperty(config, INDEX_ID_CONFIG_KEY);
            if (StringUtils.isEmpty(indexId)) {
                String indexIdFormat = ConfigUtils.getStringProperty(config, INDEX_ID_FORMAT_CONFIG_KEY, DEFAULT_INDEX_ID_FORMAT);

                indexId = String.format(indexIdFormat, siteName);
            }
        }

        if (CollectionUtils.isEmpty(batchIndexers)) {
            throw new IllegalStateException("At least one batch indexer should be provided");
        }
    }

    @Override
    public void destroy() {
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
        logger.info("Performing search indexing...");

        ChangeSet changeSet = deployment.getChangeSet();
        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();
        UpdateSet updateSet = new UpdateSet(ListUtils.union(createdFiles, updatedFiles), deletedFiles);
        UpdateStatus updateStatus = new UpdateStatus();

        execution.setStatusDetails(updateStatus);

        context = createContentStoreContext();
        try {
            if (CollectionUtils.isNotEmpty(createdFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(searchService, indexId, siteName, contentStoreService, context, updateSet, updateStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(searchService, indexId, siteName, contentStoreService, context, updateSet, updateStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(searchService, indexId, siteName, contentStoreService, context, updateSet, updateStatus);
                }
            }

            if (updateStatus.getAttemptedUpdatesAndDeletes() > 0) {
                searchService.commit(indexId);
            }
        } catch (Exception e) {
            throw new DeployerException("Error while performing search indexing", e);
        } finally {
            destroyContentStoreContext(context);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    protected Context createContentStoreContext() throws DeployerException {
        try {
            Context context = contentStoreService.createContext(FileSystemContentStoreAdapter.STORE_TYPE, null, null, null, localRepoUrl,
                                                                xmlMergingEnabled, false, 0, Context.DEFAULT_IGNORE_HIDDEN_FILES);

            logger.debug("Content store context created: {}", context);

            return context;
        } catch (Exception e) {
            throw new DeployerException("Unable to create context for content store @ " + localRepoUrl, e);
        }
    }

    protected void destroyContentStoreContext(Context context) {
        try {
            contentStoreService.destroyContext(context);

            logger.debug("Content store context destroyed: {}", context);
        } catch (Exception e) {
            logger.warn("Unable to destroy context " + context, e);
        }
    }

}
