/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.craftercms.deployer.api.Target;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.exception.ElasticsearchException;
import org.craftercms.deployer.impl.processors.AbstractSearchIndexingProcessor;
import org.craftercms.search.commons.exception.SearchException;

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

    private static final String DEFAULT_METADATA_PATH_FIELD_NAME = "metadataPath";

    protected String localIdFieldName = DEFAULT_LOCAL_ID_FIELD_NAME;

    protected String inheritsFromFieldName = DEFAULT_INHERITS_FROM_FIELD_NAME;

    protected String includedDescriptorsFieldName = DEFAULT_INCLUDED_DESCRIPTORS_FIELD_NAME;

    protected String metadataPathFieldName = DEFAULT_METADATA_PATH_FIELD_NAME;

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
            Query query = Query.of(q -> q
                .bool(b -> b
                    .filter(f -> f
                        .match(m -> m
                            .field(inheritsFromFieldName)
                            .query(v -> v
                                .stringValue(descriptorPath)
                            )
                        )
                    )
                    .mustNot(n -> n
                        .exists(e -> e
                            .field(metadataPathFieldName)
                        )
                    )
                )
            );
            return elasticsearchService.searchField(indexId, localIdFieldName, query);
        } catch (ElasticsearchException e) {
            throw new SearchException(indexId,
                "Error executing search of descriptors inheriting from " + descriptorPath, e);
        }
    }

    @Override
    protected List<String> getItemsThatIncludeComponent(final String indexId, final String componentPath) {
        try {
            Query query = Query.of(q -> q
                .bool(b -> b
                    .filter(f -> f
                        .match(m -> m
                            .field(includedDescriptorsFieldName)
                            .query(v -> v
                                .stringValue(componentPath)
                            )
                        )
                    )
                    .mustNot(n -> n
                        .exists(e -> e
                            .field(metadataPathFieldName)
                        )
                    )
                )
            );
            return elasticsearchService.searchField(indexId, localIdFieldName, query);
        } catch (ElasticsearchException e) {
            throw new SearchException(indexId,
                "Error executing search of descriptors that include component " + componentPath, e);
        }
    }

}
