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

import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.Target;

import java.util.Map;

/**
 * Extension of {@link AbstractTargetUpgradeOperation} that only runs if a given processor is found in the pipeline of
 * the target
 *
 * @author joseross
 * @since 3.1.9
 */
public abstract class ProcessorPresentUpgradeOperation extends AbstractTargetUpgradeOperation {

    @Override
    protected void doExecute(Target target, Map<String, Object> targetConfig) throws Exception {
        logger.debug("Looking for processor '{}' in pipeline for target '{}'", processorName, target.getId());
        if (processorExists(targetConfig)) {
            doExecuteInternal(target, targetConfig);
        } else {
            logger.info("Processor '{}' not found in pipeline for target '{}', operation will be skipped",
                    processorName, target.getId());
        }
    }

    private boolean processorExists(Map<String, Object> targetConfig) {
        return getPipeline(targetConfig).stream().anyMatch(processor ->
                StringUtils.equals(processor.get(CONFIG_KEY_PROCESSOR_NAME).toString(), processorName));
    }

    protected abstract void doExecuteInternal(Target target, Map<String, Object> targetConfig) throws Exception;

}
