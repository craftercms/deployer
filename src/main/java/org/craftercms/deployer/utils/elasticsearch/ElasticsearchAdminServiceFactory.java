/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.elasticsearch.ElasticsearchService;
import org.craftercms.search.elasticsearch.impl.ElasticsearchAdminServiceImpl;
import org.craftercms.search.elasticsearch.impl.MultiElasticsearchAdminServiceImpl;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.core.io.Resource;

/**
 * Implementation of {@link AbstractElasticsearchFactory} for {@link ElasticsearchAdminService}
 *
 * @author joseross
 * @since 3.1.5
 */
public class ElasticsearchAdminServiceFactory extends AbstractElasticsearchFactory<ElasticsearchAdminService> {

    /**
     * Index settings file for authoring indices
     */
    protected Resource authoringIndexSettings;

    /**
     * Index settings file for preview indices
     */
    protected Resource previewIndexSettings;

    public ElasticsearchAdminServiceFactory(final ElasticsearchConfig config,
                                            final Resource authoringIndexSettings,
                                            final Resource previewIndexSettings) {
        super(config);
        this.authoringIndexSettings = authoringIndexSettings;
        this.previewIndexSettings = previewIndexSettings;
    }

    @Override
    public Class<?> getObjectType() {
        return ElasticsearchService.class;
    }

    @Override
    protected ElasticsearchAdminService doCreateSingleInstance(final RestHighLevelClient client) {
        return new ElasticsearchAdminServiceImpl(authoringIndexSettings, previewIndexSettings, client);
    }

    @Override
    protected ElasticsearchAdminService doCreateMultiInstance(final RestHighLevelClient readClient,
                                                              final RestHighLevelClient[] writeClients) {
        return new MultiElasticsearchAdminServiceImpl(authoringIndexSettings, previewIndexSettings,
                                                      readClient, writeClients);
    }

}
