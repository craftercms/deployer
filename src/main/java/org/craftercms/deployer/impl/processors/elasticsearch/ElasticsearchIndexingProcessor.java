/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.craftercms.deployer.impl.processors.elasticsearch;

import java.beans.ConstructorProperties;
import java.util.List;

import org.craftercms.deployer.api.Target;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.deployer.impl.processors.AbstractSearchIndexingProcessor;
import org.craftercms.search.commons.exception.SearchException;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

/**
 * Implementation of {@link AbstractSearchIndexingProcessor} for Elasticsearch
 *
 * @author joseross
 * @since 3.1.0
 */
public class ElasticsearchIndexingProcessor extends AbstractSearchIndexingProcessor {

    private static final String DEFAULT_LOCAL_ID_FIELD_NAME = "localId";

    private static final String DEFAULT_INHERITS_FROM_FIELD_NAME = "inheritsFrom_smv";

    private static final String DEFAULT_INCLUDED_DESCRIPTORS_FIELD_NAME = "includedDescriptors";

    protected String localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;

    protected String inheritsFromFieldName = DEFAULT_INHERITS_FROM_FIELD_NAME;

    protected String includedDescriptorsFieldName = DEFAULT_INCLUDED_DESCRIPTORS_FIELD_NAME;

    protected ElasticsearchService elasticsearchService;

    protected ElasticsearchAdminService elasticsearchAdminService;

    @ConstructorProperties({"elasticsearchService", "elasticsearchAdminService"})
    public ElasticsearchIndexingProcessor(ElasticsearchService elasticsearchService,
                                          ElasticsearchAdminService elasticsearchAdminService) {
        this.elasticsearchService = elasticsearchService;
        this.elasticsearchAdminService = elasticsearchAdminService;
    }

    @Override
    protected void doCreateIndexIfMissing(Target target) {
        elasticsearchAdminService.createIndex(indexId);
    }

    @Override
    protected void doCommit(final String indexId) {
        try {
            elasticsearchService.refresh(indexId);
        } catch (Exception e) {
            throw new SearchException(indexId, "Error committing changes", e);
        }
    }

    @Override
    protected List<String> getItemsThatInheritDescriptor(final String indexId, final String descriptorPath) {
        try {
            return elasticsearchService.searchField(indexId, localIdFieldName, boolQuery().
                filter(matchQuery(inheritsFromFieldName, descriptorPath)));
        } catch (ElasticsearchException e) {
            throw new SearchException(indexId,
                "Error executing search of descriptors inheriting from " + descriptorPath, e);
        }
    }

    @Override
    protected List<String> getItemsThatIncludeComponent(final String indexId, final String componentPath) {
        try {
            return elasticsearchService.searchField(indexId, localIdFieldName, boolQuery()
                    .filter(termQuery(includedDescriptorsFieldName, componentPath)));
        } catch (ElasticsearchException e) {
            throw new SearchException(indexId,
                "Error executing search of descriptors that include component " + componentPath, e);
        }
    }

}
