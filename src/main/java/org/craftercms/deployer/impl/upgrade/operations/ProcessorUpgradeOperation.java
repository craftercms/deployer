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

package org.craftercms.deployer.impl.upgrade.operations;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSOR_NAME_CONFIG_KEY;

/**
 * Implementation of {@link AbstractTargetUpgradeOperation} that handles upgrades for processor properties
 *
 * @author joseross
 * @since 3.1.5
 */
public class ProcessorUpgradeOperation extends AbstractProcessorUpgradeOperation {

    /**
     * The properties to replace
     */
    protected List<Map<String, String>> replacements;

    protected List<Map<String, String>> removals;

    protected List<Map<String, Object>> additions;

    @Override
    protected void doInit(final HierarchicalConfiguration<?> config) throws ConfigurationException {
        removals = new LinkedList<>();
        for (HierarchicalConfiguration<?> removeConfig : config.configurationsAt(CONFIG_KEY_REMOVE)) {
            Map<String, String> map = new HashMap<>();
            map.put(CONFIG_KEY_PROPERTY, getRequiredStringProperty(removeConfig, CONFIG_KEY_PROPERTY));
            map.put(CONFIG_KEY_PATTERN, getRequiredStringProperty(removeConfig, CONFIG_KEY_PATTERN));
            removals.add(map);
        }
        replacements = new LinkedList<>();
        for (HierarchicalConfiguration<?> replacementConfig : config.configurationsAt(CONFIG_KEY_REPLACE)) {
            Map<String, String> map = new HashMap<>();
            map.put(CONFIG_KEY_PROPERTY, getRequiredStringProperty(replacementConfig, CONFIG_KEY_PROPERTY));
            map.put(CONFIG_KEY_PATTERN, getRequiredStringProperty(replacementConfig, CONFIG_KEY_PATTERN));
            map.put(CONFIG_KEY_EXPRESSION, getRequiredStringProperty(replacementConfig, CONFIG_KEY_EXPRESSION));
            replacements.add(map);
        }
        additions = new LinkedList<>();
        for (HierarchicalConfiguration<?> additionConfig : config.configurationsAt(CONFIG_KEY_ADD)) {
            var map = new HashMap<String, Object>();
            map.put(CONFIG_KEY_PROPERTY, getRequiredStringProperty(additionConfig, CONFIG_KEY_PROPERTY));
            if (additionConfig.containsKey(CONFIG_KEY_VALUE)) {
                map.put(CONFIG_KEY_VALUE, getRequiredStringProperty(additionConfig, CONFIG_KEY_VALUE));
            } else if (additionConfig.containsKey(CONFIG_KEY_VALUES)) {
                map.put(CONFIG_KEY_VALUES, additionConfig.getList(CONFIG_KEY_VALUES));
            }
            additions.add(map);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecuteInternal(final Target target, final Map<String, Object> targetConfig) {
        getPipeline(targetConfig).stream()
            .filter(processor -> processor.get(PROCESSOR_NAME_CONFIG_KEY).toString().matches(processorName))
            .forEach(processor -> {
                var name = processor.get(PROCESSOR_NAME_CONFIG_KEY).toString();
                logger.debug("Running removals for processor '{}' in target '{}'", name, target);
                removals.forEach(config -> {
                    String property = config.get(CONFIG_KEY_PROPERTY);
                    if (processor.containsKey(property)) {
                        String pattern = config.get(CONFIG_KEY_PATTERN);
                        String propertyValue = processor.get(property).toString();
                        if (propertyValue.matches(pattern)) {
                            logger.trace("Removing property '{}' for processor '{}' in target '{}'", property, name,
                                    target);
                            processor.remove(property);
                        }
                    }
                });
                logger.debug("Running replacements for processor '{}' in target '{}'", name, target);
                replacements.forEach(config -> {
                    String property = config.get(CONFIG_KEY_PROPERTY);
                    if (processor.containsKey(property)) {
                        String pattern = config.get(CONFIG_KEY_PATTERN);
                        String expression = config.get(CONFIG_KEY_EXPRESSION);
                        logger.trace("Replacing property '{}' for processor '{}' in target '{}'", property, name,
                                target);
                        processor.put(property, processor.get(property).toString().replaceAll(pattern, expression));
                    }
                });
                logger.debug("Running additions for processor '{}' in target '{}'", name, target);
                additions.forEach(config -> {
                    var property = (String) config.get(CONFIG_KEY_PROPERTY);
                    if (config.containsKey(CONFIG_KEY_VALUE)) {
                        processor.put(property, config.get(CONFIG_KEY_VALUE));
                    } else {
                        if (processor.containsKey(property)) {
                            List<Object> list = (List<Object>) processor.get(property);
                            list.addAll((List<Object>) config.get(CONFIG_KEY_VALUES));
                        } else {
                            processor.put(property, config.get(CONFIG_KEY_VALUES));
                        }
                    }
                });
            });
    }

}
