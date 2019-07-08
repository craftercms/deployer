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
package org.craftercms.deployer.impl.lifecycle;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.search.service.AdminService;
import org.springframework.beans.factory.annotation.Required;

import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Base abstract {@link TargetLifecycleHook} for search index related hooks.
 *
 * @author avasquez
 */
public abstract class AbstractIndexAwareLifecycleHook implements TargetLifecycleHook {

    protected static final String INDEX_ID_CONFIG_KEY = "indexId";

    protected String siteName;
    protected String indexIdFormat;
    protected AdminService crafterSearchAdminService;
    protected ElasticsearchAdminService elasticsearchAdminService;

    // Config properties (populated on init)

    protected String indexId;

    @Required
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @Required
    public void setIndexIdFormat(String indexIdFormat) {
        this.indexIdFormat = indexIdFormat;
    }

    @Required
    public void setCrafterSearchAdminService(AdminService crafterSearchAdminService) {
        this.crafterSearchAdminService = crafterSearchAdminService;
    }

    @Required
    public void setElasticsearchAdminService(ElasticsearchAdminService elasticsearchAdminService) {
        this.elasticsearchAdminService = elasticsearchAdminService;
    }

    @Override
    public void init(Configuration config) throws ConfigurationException {
        indexId = getStringProperty(config, INDEX_ID_CONFIG_KEY);
        if (StringUtils.isEmpty(indexId)) {
            indexId = String.format(indexIdFormat, siteName);
        }
    }

}
