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

import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the scan for new/updated targets.
 * @author joseross
 */
@Component
@ConditionalOnProperty("deployer.main.targets.scan.scheduling.enabled")
public class TargetScanTask {

    private static final Logger logger = LoggerFactory.getLogger(TargetScanTask.class);

    @Autowired
    protected TargetService targetService;

    /**
     * Scans for new/updated targets.
     */
    @Scheduled(cron = "${deployer.main.targets.scan.scheduling.cron}")
    public void scanTargets() {
        try {
            targetService.resolveTargets();
        } catch (DeployerException e) {
            logger.error("Scheduled target scan failed", e);
        }
    }

}
