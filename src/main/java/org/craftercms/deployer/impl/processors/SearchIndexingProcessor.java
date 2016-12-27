package org.craftercms.deployer.impl.processors;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.service.SearchService;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.deployer.impl.GlobalConfigurationProperties.*;

/**
 * Created by alfonsovasquez on 12/26/16.
 */
public class SearchIndexingProcessor implements DeploymentProcessor {

    public static final String DEFAULT_INDEX_ID_FORMAT = "%s-default";

    public static final String INDEX_ID_PROPERTY = "indexId";
    public static final String SITE_NAME_PROPERTY = "siteName";
    public static final String INDEX_ID_FORMAT_PROPERTY = "indexIdFormat";
    public static final String IGNORE_INDEX_ID_PROPERTY = "ignoreIndexId";

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
    public void init(Configuration globalConfig, Configuration processorConfig) throws DeploymentException {
        rootFolder = ConfigurationUtils.getRequiredString(globalConfig, DEPLOYMENT_ROOT_FOLDER_PROPERTY_NAME);
        indexId = ConfigurationUtils.getString(processorConfig, INDEX_ID_PROPERTY);
        siteName = ConfigurationUtils.getString(processorConfig, SITE_NAME_PROPERTY);

        if (StringUtils.isEmpty(siteName)) {
            siteName = ConfigurationUtils.getRequiredString(globalConfig, DEPLOYMENT_ID_PROPERTY_NAME);
        }

        boolean ignoreIndexId = ConfigurationUtils.getBoolean(processorConfig, IGNORE_INDEX_ID_PROPERTY, false);
        String indexIdFormat = ConfigurationUtils.getString(processorConfig, INDEX_ID_FORMAT_PROPERTY, DEFAULT_INDEX_ID_FORMAT);

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
    public ChangeSet execute(DeploymentContext context, ChangeSet changeSet) throws DeploymentException {
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
        }

        return changeSet;
    }

}
