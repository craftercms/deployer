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

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.search.commons.exception.SearchException;

/**
 * Implementation of {@link TargetLifecycleHook} that deletes an Elasticsearch index or a Crafter Search
 * based index.
 *
 * @author avasquez
 */
public class DeleteIndexLifecycleHook extends AbstractIndexAwareLifecycleHook {

    @Override
    public void doExecute(Target target) throws DeployerException {
        try {
            logger.info("Deleting Elasticsearch index for target '{}'", target.getId());

            openSearchAdminService.deleteIndexes(indexId);
        } catch (SearchException e) {
            throw new DeployerException("Error creating index for target " + target.getId(), e);
        }
    }

}
