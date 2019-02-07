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

package org.craftercms.deployer.impl.processors.elasticsearch;

import java.util.List;

import org.craftercms.search.elasticsearch.ElasticSearchService;
import org.craftercms.search.elasticsearch.exception.ElasticSearchException;
import org.craftercms.deployer.impl.processors.AbstractSearchIndexingProcessor;
import org.craftercms.search.exception.SearchException;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.springframework.beans.factory.annotation.Required;

/**
 * @author joseross
 */
public class ElasticSearchIndexingProcessor extends AbstractSearchIndexingProcessor {

    protected ElasticSearchService elasticSearchService;

    @Required
    public void setElasticSearchService(final ElasticSearchService elasticSearchService) {
        this.elasticSearchService = elasticSearchService;
    }

    @Override
    protected void doCommit(final String indexId) {
        try {
            elasticSearchService.flush(indexId);
        } catch (Exception e) {
            throw new SearchException(indexId, "Error committing changes", e);
        }
    }

    @Override
    protected List<String> getItemsThatIncludeComponent(final String indexId, final String componentPath) {
        try {
            return elasticSearchService.searchField(indexId, "localId",
                new BoolQueryBuilder()
                    .filter(new TermQueryBuilder("includedDescriptors", componentPath))
            );
        } catch (ElasticSearchException e) {
            throw new SearchException(indexId, "Error executing search for " + componentPath, e);
        }
    }

}
