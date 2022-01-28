/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.deployer.utils.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.craftercms.search.elasticsearch.DocumentParser;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.impl.ElasticsearchDocumentBuilder;
import org.craftercms.search.elasticsearch.impl.ElasticsearchServiceImpl;
import org.craftercms.search.elasticsearch.impl.MultiElasticsearchServiceImpl;

import java.beans.ConstructorProperties;

/**
 * Implementation of {@link org.craftercms.deployer.utils.elasticsearch.legacy.AbstractElasticsearchFactory}
 * for {@link ElasticsearchService}
 *
 * @author joseross
 * @since 3.1.5
 */
public class ElasticsearchServiceFactory extends AbstractElasticsearchFactory<ElasticsearchService> {

    /**
     * The document builder
     */
    protected ElasticsearchDocumentBuilder documentBuilder;

    /**
     * The document parser
     */
    protected DocumentParser documentParser;

    @ConstructorProperties({"config", "documentBuilder", "documentParser"})
    public ElasticsearchServiceFactory(final ElasticsearchConfig config,
                                       final ElasticsearchDocumentBuilder documentBuilder,
                                       final DocumentParser documentParser) {
        super(config);
        this.documentBuilder = documentBuilder;
        this.documentParser = documentParser;
    }

    @Override
    public Class<?> getObjectType() {
        return ElasticsearchService.class;
    }

    @Override
    protected ElasticsearchService doCreateSingleInstance(final ElasticsearchClient client) {
        return new ElasticsearchServiceImpl(documentBuilder, documentParser, client);
    }

    @Override
    protected ElasticsearchService doCreateMultiInstance(final ElasticsearchClient readClient,
                                                         final ElasticsearchClient[] writeClients) {
        return new MultiElasticsearchServiceImpl(documentBuilder, documentParser, readClient, writeClients);
    }

}
