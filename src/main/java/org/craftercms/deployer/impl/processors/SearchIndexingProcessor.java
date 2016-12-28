package org.craftercms.deployer.impl.processors;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.service.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.deployer.impl.CommonConfigurationProperties.DEPLOYMENT_ROOT_FOLDER_PROPERTY_NAME;

/**
 * Created by alfonsovasquez on 12/26/16.
 */
public class SearchIndexingProcessor extends AbstractDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexingProcessor.class);

    public static final String DEFAULT_INDEX_ID_FORMAT = "%s-default";

    public static final String INDEX_ID_PROPERTY_NAME = "indexId";
    public static final String SITE_NAME_PROPERTY_NAME = "siteName";
    public static final String INDEX_ID_FORMAT_PROPERTY_NAME = "indexIdFormat";
    public static final String IGNORE_INDEX_ID_PROPERTY_NAME = "ignoreIndexId";

    protected String rootFolder;
    protected String indexId;
    protected String siteName;
    protected SearchService searchService;
    protected List<BatchIndexer> batchIndexers;

    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setBatchIndexer(BatchIndexer batchIndexer) {
        this.batchIndexers = Collections.singletonList(batchIndexer);
    }

    public void setBatchIndexers(List<BatchIndexer> batchIndexers) {
        this.batchIndexers = batchIndexers;
    }

    @Override
    public void doInit(Configuration mainConfig, Configuration processorConfig) throws DeploymentException {
        rootFolder = ConfigurationUtils.getRequiredString(mainConfig, DEPLOYMENT_ROOT_FOLDER_PROPERTY_NAME);
        indexId = ConfigurationUtils.getString(processorConfig, INDEX_ID_PROPERTY_NAME);
        siteName = ConfigurationUtils.getString(processorConfig, SITE_NAME_PROPERTY_NAME);

        if (StringUtils.isEmpty(siteName)) {
            siteName = deploymentId;
        }

        boolean ignoreIndexId = ConfigurationUtils.getBoolean(processorConfig, IGNORE_INDEX_ID_PROPERTY_NAME, false);
        String indexIdFormat = ConfigurationUtils.getString(processorConfig, INDEX_ID_FORMAT_PROPERTY_NAME, DEFAULT_INDEX_ID_FORMAT);

        if (ignoreIndexId) {
            indexId = null;
        } else if (StringUtils.isEmpty(indexId)) {
            indexId = String.format(indexIdFormat, siteName);
        }

        if (CollectionUtils.isEmpty(batchIndexers)) {
            throw new IllegalStateException("At least one batch indexer should be provided");
        }
    }

    @Override
    public void destroy() throws DeploymentException {
    }

    @Override
    public ChangeSet doExecute(DeploymentContext context, ChangeSet changeSet) throws DeploymentException {
        logger.info("Performing search indexing...");

        try {
            List<String> createdFiles = changeSet.getCreatedFiles();
            List<String> updatedFiles = changeSet.getUpdatedFiles();
            List<String> deletedFiles = changeSet.getDeletedFiles();
            int updateCount = 0;

            if (CollectionUtils.isNotEmpty(createdFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    updateCount += indexer.updateIndex(indexId, siteName, rootFolder, createdFiles, false);
                }
            }
            if (CollectionUtils.isNotEmpty(updatedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    updateCount += indexer.updateIndex(indexId, siteName, rootFolder, updatedFiles, false);
                }
            }
            if (CollectionUtils.isNotEmpty(deletedFiles)) {
                for (BatchIndexer indexer : batchIndexers) {
                    updateCount += indexer.updateIndex(indexId, siteName, rootFolder, deletedFiles, true);
                }
            }

            if (updateCount > 0) {
                searchService.commit(indexId);
            } else {
                logger.info("No files indexed");
            }

            return changeSet;
        } catch (Exception e) {
            throw new DeploymentException("Error while performing search indexing", e);
        }
    }

}
