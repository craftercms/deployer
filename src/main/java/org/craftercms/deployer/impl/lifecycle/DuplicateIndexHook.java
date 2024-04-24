/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.search.opensearch.OpenSearchAdminService;

import java.beans.ConstructorProperties;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Implementation of {@link org.craftercms.deployer.api.lifecycle.TargetLifecycleHook} that duplicates an index from
 * a source site to a target site.
 */
public class DuplicateIndexHook extends AbstractIndexAwareLifecycleHook {
    private final String sourceSiteName;

    @ConstructorProperties({"siteName", "indexIdFormat", "searchAdminService", "sourceSiteName"})
    public DuplicateIndexHook(String siteName, String indexIdFormat, OpenSearchAdminService searchAdminService, String sourceSiteName) {
        super(siteName, indexIdFormat, searchAdminService);
        this.sourceSiteName = sourceSiteName;
    }

    @Override
    protected void doExecute(Target target) throws DeployerException {
        if (isEmpty(sourceSiteName)) {
            throw new DeployerException("'target.sourceSiteName' is required for target duplication");
        }
        String sourceIndexIdFormat = target.getConfiguration().getString("target.search.indexIdFormat");
        String sourceIndexId = format(sourceIndexIdFormat, sourceSiteName);
        logger.info("Starting index duplicate from '{}' for site '{}' to '{}' for site '{}'", sourceIndexId, sourceSiteName, indexId, siteName);
        searchAdminService.duplicateIndex(sourceIndexId, indexId);
        logger.info("Completed index duplicate from '{}' for site '{}' to '{}' for site '{}'", sourceIndexId, sourceSiteName, indexId, siteName);
    }
}
