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
package org.craftercms.deployer.impl.lifecycle;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.search.opensearch.OpenSearchAdminService;

import java.beans.ConstructorProperties;

import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Base abstract {@link TargetLifecycleHook} for search index related hooks.
 *
 * @author avasquez
 */
public abstract class AbstractIndexAwareLifecycleHook extends AbstractLifecycleHook {

    protected static final String INDEX_ID_CONFIG_KEY = "indexId";

    protected final String siteName;
    protected final String indexIdFormat;
    protected final OpenSearchAdminService searchAdminService;

    // Config properties (populated on init)

    protected String indexId;

    @ConstructorProperties({"siteName", "indexIdFormat", "searchAdminService"})
    public AbstractIndexAwareLifecycleHook(final String siteName, final String indexIdFormat,
                                           final OpenSearchAdminService searchAdminService) {
        this.siteName = siteName;
        this.indexIdFormat = indexIdFormat;
        this.searchAdminService = searchAdminService;
    }

    @Override
    public void doInit(Configuration config) throws ConfigurationException {
        indexId = getStringProperty(config, INDEX_ID_CONFIG_KEY);
        if (StringUtils.isEmpty(indexId)) {
            indexId = String.format(indexIdFormat, siteName);
        }
    }

}
