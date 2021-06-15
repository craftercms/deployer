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

import java.util.Map;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link ConditionalEnvUpgradeOperation} that removes a property from the target's configuration
 *
 * @author joseross
 * @since 4.0.0
 */
public class RemovePropertyUpgradeOperation extends ConditionalEnvUpgradeOperation {

    public static final String CONFIG_KEY_PROPERTY_NAME = "property";

    /**
     * The name of the property
     */
    protected String propertyName;

    @Override
    protected void doInit(HierarchicalConfiguration<?> config) throws ConfigurationException {
        propertyName = getRequiredStringProperty(config, CONFIG_KEY_PROPERTY_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecuteInternal(Target target, Map<String, Object> targetConfig) {
        Map<String, Object> currentMap = targetConfig;
        String[] names = propertyName.split("\\.");
        if (names.length > 1) {
            for (int i = 0; i < names.length - 1; i++) {
                currentMap = (Map<String, Object>) currentMap.get(names[i]);
            }
        }
        currentMap.remove(names[names.length - 1]);
    }

}
