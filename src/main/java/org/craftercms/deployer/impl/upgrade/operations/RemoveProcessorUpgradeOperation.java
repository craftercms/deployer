/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;

import java.util.*;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSOR_NAME_CONFIG_KEY;

/**
 * Operation to remove a processor from the pipeline of the target
 */
public class RemoveProcessorUpgradeOperation  extends AbstractProcessorUpgradeOperation {
    /**
     * The additional properties a processor must contain for removal
     */
    protected Map<String, String> processorConfiguration;

    @Override
    protected void doInit(HierarchicalConfiguration<?> config) throws ConfigurationException {
        processorConfiguration = new HashMap<>();
        for (HierarchicalConfiguration<?> additionConfig : config.configurationsAt(CONFIG_KEY_PROPERTIES)) {
            var property = getRequiredStringProperty(additionConfig, CONFIG_KEY_PROPERTY);
            if (additionConfig.containsKey(CONFIG_KEY_VALUE)) {
                processorConfiguration.put(property, getRequiredStringProperty(additionConfig, CONFIG_KEY_VALUE));
            } else if (additionConfig.containsKey(CONFIG_KEY_VALUES)) {
                processorConfiguration.put(property, additionConfig.getList(CONFIG_KEY_VALUES).toString());
            }
        }
    }

    /**
     * Check if the processor has all the required properties
     * @param processorObj processor object
     * @return true if all properties are the same, false otherwise
     */
    protected boolean hasAllProperties(Map<String, Object> processorObj) {
        if (!processorObj.get(PROCESSOR_NAME_CONFIG_KEY).equals(processorName)) {
            return false;
        }

        for (String property : processorConfiguration.keySet()) {
            if (!(processorObj.containsKey(property) &&
                    processorObj.get(property).toString().equalsIgnoreCase(processorConfiguration.get(property)))) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void doExecuteInternal(Target target, Map<String, Object> targetConfig) {
        var pipeline = getPipeline(targetConfig);
        var removeList = pipeline
                .stream()
                .filter(processor -> hasAllProperties(processor))
                .toList();
        pipeline.removeAll(removeList);
    }
}
