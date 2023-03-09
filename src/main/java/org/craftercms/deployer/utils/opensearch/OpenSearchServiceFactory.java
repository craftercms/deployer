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

package org.craftercms.deployer.utils.opensearch;

import org.craftercms.deployer.utils.opensearch.legacy.AbstractOpenSearchFactory;
import org.craftercms.search.opensearch.DocumentParser;
import org.craftercms.search.opensearch.OpenSearchService;
import org.craftercms.search.opensearch.impl.MultiOpenSearchServiceImpl;
import org.craftercms.search.opensearch.impl.OpenSearchDocumentBuilder;
import org.craftercms.search.opensearch.impl.OpenSearchServiceImpl;
import org.opensearch.client.opensearch.OpenSearchClient;

import java.beans.ConstructorProperties;

/**
 * Implementation of {@link AbstractOpenSearchFactory}
 * for {@link OpenSearchService}
 *
 * @author joseross
 * @since 3.1.5
 */
public class OpenSearchServiceFactory extends AbstractElasticsearchFactory<OpenSearchService> {

    /**
     * The document builder
     */
    protected OpenSearchDocumentBuilder documentBuilder;

    /**
     * The document parser
     */
    protected DocumentParser documentParser;

    @ConstructorProperties({"config", "documentBuilder", "documentParser"})
    public OpenSearchServiceFactory(final OpenSearchConfig config,
                                    final OpenSearchDocumentBuilder documentBuilder,
                                    final DocumentParser documentParser) {
        super(config);
        this.documentBuilder = documentBuilder;
        this.documentParser = documentParser;
    }

    @Override
    public Class<?> getObjectType() {
        return OpenSearchService.class;
    }

    @Override
    protected OpenSearchService doCreateSingleInstance(final OpenSearchClient client) {
        return new OpenSearchServiceImpl(documentBuilder, documentParser, client);
    }

    @Override
    protected OpenSearchService doCreateMultiInstance(final OpenSearchClient readClient,
                                                      final OpenSearchClient[] writeClients) {
        return new MultiOpenSearchServiceImpl(documentBuilder, documentParser, readClient, writeClients);
    }

}
