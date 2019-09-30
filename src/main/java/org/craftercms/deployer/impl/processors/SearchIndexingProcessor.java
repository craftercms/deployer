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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.craftercms.search.batch.BatchIndexer;
import org.craftercms.search.service.Query;
import org.craftercms.search.service.SearchService;
import org.craftercms.search.service.impl.SolrQuery;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author joseross
 */
public class SearchIndexingProcessor extends AbstractSearchIndexingProcessor {

    protected static final String LOCAL_ID_FIELD = "localId";
    protected static final String SEARCH_RESULTS_RESPONSE_PROPERTY = "response";
    protected static final String SEARCH_RESULTS_NUM_FOUND_PROPERTY = "numFound";
    protected static final String SEARCH_RESULTS_DOCUMENTS_PROPERTY = "documents";

    protected static final String DEFAULT_ITEMS_THAT_INHERIT_FROM_DESCRIPTOR_QUERY_FORMAT = "inheritsFrom_smv:\"%s\"";
    protected static final String DEFAULT_ITEMS_THAT_INCLUDE_COMPONENT_QUERY_FORMAT = "includedDescriptors:\"%s\"";

    protected String itemsThatInheritFromDescriptorQueryFormat;
    protected String itemsThatIncludeComponentQueryFormat;

    protected SearchService searchService;

    public SearchIndexingProcessor() {
        this.itemsThatInheritFromDescriptorQueryFormat = DEFAULT_ITEMS_THAT_INHERIT_FROM_DESCRIPTOR_QUERY_FORMAT;
        this.itemsThatIncludeComponentQueryFormat = DEFAULT_ITEMS_THAT_INCLUDE_COMPONENT_QUERY_FORMAT;
    }

    /**
     * Sets the search service. Since all indexing is done through the {@link BatchIndexer}s the search service is
     * only used
     * to commit.
     */
    @Required
    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Sets the format of the search query used to find items that include components (used when
     * {@code reindexItemsOnComponentUpdates} is enabled).
     */
    public void setItemsThatIncludeComponentQueryFormat(String itemsThatIncludeComponentQueryFormat) {
        this.itemsThatIncludeComponentQueryFormat = itemsThatIncludeComponentQueryFormat;
    }

    @Override
    protected void doCommit(final String indexId) {
        searchService.commit(indexId);
    }

    @Override
    protected List<String> getItemsThatInheritDescriptor(final String indexId, final String descriptorPath) {
        return searchField(indexId, descriptorPath, createItemsThatInheritFromDescriptorQuery(descriptorPath));
    }

    protected Query createItemsThatInheritFromDescriptorQuery(String descriptorPath) {
        String queryStatement = String.format(itemsThatInheritFromDescriptorQueryFormat, descriptorPath);
        SolrQuery query = new SolrQuery();

        query.setQuery(queryStatement);
        query.setFieldsToReturn(LOCAL_ID_FIELD);

        return query;
    }

    protected Query createItemsThatIncludeComponentQuery(String componentId) {
        String queryStatement = String.format(itemsThatIncludeComponentQueryFormat, componentId);
        SolrQuery query = new SolrQuery();

        query.setQuery(queryStatement);
        query.setFieldsToReturn(LOCAL_ID_FIELD);

        return query;
    }

    protected List<String> getItemsThatIncludeComponent(String indexId, String componentPath) {
        return searchField(indexId, componentPath, createItemsThatIncludeComponentQuery(componentPath));
    }

    @SuppressWarnings("unchecked")
    protected List<String> searchField(String indexId, String componentPath, Query query) {
        List<String> items = new ArrayList<>();
        int start = 0;
        int rows = itemsThatIncludeComponentQueryRows;
        int count;
        Map<String, Object> result;
        Map<String, Object> response;
        List<Map<String, Object>> documents;

        do {
            query.setOffset(start);
            query.setNumResults(rows);

            result = searchService.search(indexId, query);
            response = (Map<String, Object>)result.get(SEARCH_RESULTS_RESPONSE_PROPERTY);
            count = (int)response.get(SEARCH_RESULTS_NUM_FOUND_PROPERTY);
            documents = (List<Map<String, Object>>)response.get(SEARCH_RESULTS_DOCUMENTS_PROPERTY);

            for (Map<String, Object> document : documents) {
                items.add((String)document.get(LOCAL_ID_FIELD));
            }

            start += rows;
        } while (start <= count);

        return items;
    }

}
