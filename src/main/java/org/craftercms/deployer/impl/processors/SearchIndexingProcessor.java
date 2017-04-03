package org.craftercms.deployer.impl.processors;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.exception.CrafterException;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.store.impl.filesystem.FileSystemContentStoreAdapter;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.batch.IndexingStatus;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonsovasquez on 12/26/16.
 */
public class SearchIndexingProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingProcessor.class);

    public static final String DEFAULT_INDEX_ID_FORMAT = "%s";
    public static final int DEFAULT_BATCH_SIZE = 10;

    public static final String INDEX_ID_CONFIG_KEY = "indexId";
    public static final String INDEX_ID_FORMAT_CONFIG_KEY = "indexIdFormat";
    public static final String IGNORE_INDEX_ID_CONFIG_KEY = "ignoreIndexId";
    public static final String BATCH_SIZE_CONFIG_KEY = "batchSize";

    protected String localRepoUrl;
    protected ContentStoreService contentStoreService;
    protected SearchService searchService;
    protected List<BatchIndexer> batchIndexers;
    protected boolean mergingEnabled;
    protected String indexId;
    protected int batchSize;
    protected Context context;

    @Required
    public void setLocalRepoUrl(String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    @Required
    public void setContentStoreService(ContentStoreService contentStoreService) {
        this.contentStoreService = contentStoreService;
    }

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setBatchIndexer(BatchIndexer batchIndexer) {
        this.batchIndexers = Collections.singletonList(batchIndexer);
    }

    public void setMergingEnabled(boolean mergingEnabled) {
        this.mergingEnabled = mergingEnabled;
    }

    public void setBatchIndexers(List<BatchIndexer> batchIndexers) {
        this.batchIndexers = batchIndexers;
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

        batchSize = ConfigUtils.getIntegerProperty(config, BATCH_SIZE_CONFIG_KEY, DEFAULT_BATCH_SIZE);

        if (CollectionUtils.isEmpty(batchIndexers)) {
            throw new IllegalStateException("At least one batch indexer should be provided");
        }

        context = createContentStoreContext();

        logger.debug("Content store context created: {}", context);
    }

    @Override
    public void destroy() {
        destroyContentStoreContext(context);

        logger.debug("Content store context destroyed: {}", context);
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet, Map<String, Object> params) throws DeployerException {
        logger.info("Performing search indexing...");

        ChangeSet changeSet = deployment.getChangeSet();
        List<String> createdFiles = changeSet.getCreatedFiles();
        List<String> updatedFiles = changeSet.getUpdatedFiles();
        List<String> deletedFiles = changeSet.getDeletedFiles();
        IndexingStatus indexingStatus = new IndexingStatus();

        execution.setStatusDetails(indexingStatus);

        try {
            if (CollectionUtils.isNotEmpty(createdFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, contentStoreService, context, createdFiles, false, indexingStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, contentStoreService, context, updatedFiles, false, indexingStatus);
                }
            }
            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    indexer.updateIndex(indexId, siteName, contentStoreService, context, deletedFiles, true, indexingStatus);
                }
            }

            if (indexingStatus.getAttemptedUpdatesAndDeletes() > 0) {
                searchService.commit(indexId);
            }
        } catch (Exception e) {
            throw new DeployerException("Error while performing search indexing", e);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    protected Context createContentStoreContext() throws DeployerException {
        try {
            return contentStoreService.createContext(FileSystemContentStoreAdapter.STORE_TYPE, null, null, null, localRepoUrl,
                                                     mergingEnabled, false, 0, Context.DEFAULT_IGNORE_HIDDEN_FILES);
        } catch (Exception e) {
            throw new DeployerException("Unable to create context for content store @ " + localRepoUrl, e);
        }
    }

    protected void destroyContentStoreContext(Context context) {
        try {
            contentStoreService.destroyContext(context);
        } catch (Exception e) {
            logger.warn("Unable to destroy context " + context, e);
        }
    }

}
