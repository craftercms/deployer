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

package org.craftercms.deployer.impl.upgrade.operations;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.commons.upgrade.impl.operations.AbstractUpgradeOperation;
import org.craftercms.deployer.api.Target;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link org.craftercms.commons.upgrade.UpgradeOperation} that recreates an index Elasticsearch
 *
 * @author joseross
 * @since 3.1.8
 */
public class ElasticsearchIndexUpgradeOperation extends AbstractUpgradeOperation<Target> {

    protected static final String INDEX_ID_FORMAT_CONFIG_KEY = "target.search.indexIdFormat";

    @Override
    protected void doExecute(Target target) throws Exception {
        if (target.isCrafterSearchEnabled()) {
            logger.info("Target {} does not use Elasticsearch so will be skipped", target.getId());
            return;
        }
        ElasticsearchAdminService adminService = (ElasticsearchAdminService)
                target.getApplicationContext().getBean("elasticsearchAdminService");
        HierarchicalConfiguration<?> config = target.getConfiguration();
        String siteName = target.getSiteName();
        String indexIdFormat = getRequiredStringProperty(config, INDEX_ID_FORMAT_CONFIG_KEY);
        String aliasName = String.format(indexIdFormat, siteName);

        adminService.waitUntilReady();
        adminService.recreateIndex(aliasName);
    }

}
