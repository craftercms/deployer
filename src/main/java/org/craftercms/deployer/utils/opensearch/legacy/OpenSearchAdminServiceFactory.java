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

package org.craftercms.deployer.utils.opensearch.legacy;

import org.craftercms.search.opensearch.OpenSearchAdminService;
import org.craftercms.search.opensearch.impl.MultiOpenSearchAdminServiceImpl;
import org.craftercms.search.opensearch.impl.OpenSearchAdminServiceImpl;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.core.io.Resource;

import java.beans.ConstructorProperties;

/**
 * Implementation of {@link AbstractOpenSearchFactory} for {@link OpenSearchAdminService}
 *
 * @author joseross
 * @since 3.1.5
 */
public class OpenSearchAdminServiceFactory extends AbstractOpenSearchFactory<OpenSearchAdminService> {

    /**
     * Index mapping file for authoring indices
     */
    protected Resource authoringMapping;

    /**
     * Index mapping file for preview indices
     */
    protected Resource previewMapping;

    protected String authoringNamePattern;

    @ConstructorProperties({"config", "authoringMapping", "previewMapping", "authoringNamePattern"})
    public OpenSearchAdminServiceFactory(OpenSearchConfig config, Resource authoringMapping,
                                         Resource previewMapping, String authoringNamePattern) {
        super(config);
        this.authoringMapping = authoringMapping;
        this.previewMapping = previewMapping;
        this.authoringNamePattern = authoringNamePattern;
    }

    @Override
    public Class<?> getObjectType() {
        return OpenSearchAdminService.class;
    }

    @Override
    protected OpenSearchAdminService doCreateSingleInstance(final RestHighLevelClient client) {
        OpenSearchAdminServiceImpl openSearchAdminService = new OpenSearchAdminServiceImpl(
                authoringMapping, previewMapping, authoringNamePattern, config.getLocaleMapping(),
                config.indexSettings, config.ignoredSettings, client);
        openSearchAdminService.setReindexSlices(config.reindexSlices);
        openSearchAdminService.setReindexTimeoutSeconds(config.reindexTimeoutSeconds);
        return openSearchAdminService;
    }

    @Override
    protected OpenSearchAdminService doCreateMultiInstance(final RestHighLevelClient readClient,
                                                           final RestHighLevelClient[] writeClients) {
        MultiOpenSearchAdminServiceImpl openSearchAdminService = new MultiOpenSearchAdminServiceImpl(
                authoringMapping, previewMapping, authoringNamePattern, config.getLocaleMapping(), readClient,
                config.indexSettings, config.ignoredSettings, writeClients);
        openSearchAdminService.setReindexSlices(config.reindexSlices);
        openSearchAdminService.setReindexTimeoutSeconds(config.reindexTimeoutSeconds);
        return openSearchAdminService;
    }

}
