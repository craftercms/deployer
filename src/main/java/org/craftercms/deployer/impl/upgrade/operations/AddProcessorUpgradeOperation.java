/*
 * Copyright (C) 2007-2021 Crafter Software Corporation. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Operation to add a new processor to the pipeline of the target
 *
 * @author joseross
 * @since 4.0
 */
public class AddProcessorUpgradeOperation  extends AbstractProcessorUpgradeOperation {

    /**
     * The name of the processor to search to insert the new one
     */
    protected String beforeProcessor;

    /**
     * The additional properties for the new processor
     */
    protected Map<String, Object> processorConfiguration;

    @Override
    protected void doInit(HierarchicalConfiguration<?> config) throws ConfigurationException {
        beforeProcessor = getRequiredStringProperty(config, "beforeProcessor");
        processorConfiguration = new HashMap<>();
        for (HierarchicalConfiguration<?> additionConfig : config.configurationsAt("properties")) {
            var property = getRequiredStringProperty(additionConfig, CONFIG_KEY_PROPERTY);
            if (additionConfig.containsKey(CONFIG_KEY_VALUE)) {
                processorConfiguration.put(property, getRequiredStringProperty(additionConfig, CONFIG_KEY_VALUE));
            } else if (additionConfig.containsKey(CONFIG_KEY_VALUES)) {
                processorConfiguration.put(property, additionConfig.getList(CONFIG_KEY_VALUES));
            }
        }
    }

    @Override
    protected void doExecuteInternal(Target target, Map<String, Object> targetConfig) {
        // get the current pipeline
        var pipeline = getPipeline(targetConfig);

        // find the position to add the new processor
        int i;
        for (i = 0 ; i < pipeline.size(); i++) {
            if (pipeline.get(i).get(CONFIG_KEY_PROCESSOR_NAME).equals(beforeProcessor)) {
                break;
            }
        }
        // if the processor was not found, adjust the position to be the last one
        if (i >= pipeline.size()) {
            i--;
        }

        // add the new processor to the pipeline
        var newProcessor = new HashMap<String, Object>();
        newProcessor.put(CONFIG_KEY_PROCESSOR_NAME, processorName);
        newProcessor.putAll(processorConfiguration);

        pipeline.add(i, newProcessor);
    }
}
