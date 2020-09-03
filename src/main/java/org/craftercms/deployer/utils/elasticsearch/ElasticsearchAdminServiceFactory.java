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
     * Index mapping file for authoring indices
     */
    protected Resource authoringMapping;

    /**
     * Index mapping file for preview indices
     */
    protected Resource previewMapping;

    protected String authoringNamePattern;

    public ElasticsearchAdminServiceFactory(ElasticsearchConfig config, Resource authoringMapping,
                                            Resource previewMapping, String authoringNamePattern) {
        super(config);
        this.authoringMapping = authoringMapping;
        this.previewMapping = previewMapping;
        this.authoringNamePattern = authoringNamePattern;
    }

    @Override
    public Class<?> getObjectType() {
        return ElasticsearchService.class;
    }

    @Override
    protected ElasticsearchAdminService doCreateSingleInstance(final RestHighLevelClient client) {
        return new ElasticsearchAdminServiceImpl(
                authoringMapping, previewMapping, authoringNamePattern, config.getLocaleMapping(),
                config.indexSettings, client);
    }

    @Override
    protected ElasticsearchAdminService doCreateMultiInstance(final RestHighLevelClient readClient,
                                                              final RestHighLevelClient[] writeClients) {
        return new MultiElasticsearchAdminServiceImpl(
                authoringMapping, previewMapping, authoringNamePattern, config.getLocaleMapping(), readClient,
                config.indexSettings, writeClients);
    }

}
