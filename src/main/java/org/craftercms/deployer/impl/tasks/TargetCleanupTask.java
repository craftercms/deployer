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

package org.craftercms.deployer.impl.tasks;

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the cleanup of all target repositories.
 * @author joseross
 */
@Component
@ConditionalOnProperty("deployer.main.targets.cleanup.enabled")
public class TargetCleanupTask {

    private static final Logger logger = LoggerFactory.getLogger(TargetCleanupTask.class);

    @Autowired
    protected TargetService targetService;

    /**
     * Performs a cleanup on all loaded targets.
     */
    @Scheduled(cron = "${deployer.main.targets.cleanup.cron}")
    public void cleanupAllTargets() {
        try {
            logger.info("Starting cleanup for all targets");
            targetService.getAllTargets().forEach(Target::cleanRepo);
        } catch (TargetServiceException e) {
            logger.error("Error getting loaded targets", e);
        }
    }

}
