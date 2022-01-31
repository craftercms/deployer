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
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link ProcessorPresentUpgradeOperation} that adds a Lifecycle Hook if missing
 *
 * @author joseross
 * @since 3.1.9
 */
public class AddLifecycleHookUpgradeOperation extends ProcessorPresentUpgradeOperation {

    public static final String CONFIG_KEY_HOOK_TYPE = "hookType";

    protected String hookType;
    protected String hookName;

    @Override
    protected void doInit(HierarchicalConfiguration<?> config) throws ConfigurationException {
        hookType = getRequiredStringProperty(config, CONFIG_KEY_HOOK_TYPE);
        hookName = getRequiredStringProperty(config, CONFIG_KEY_HOOK_NAME);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecuteInternal(Target target, Map<String, Object> targetConfig) {
        Map<String, Object> targetObj = (Map<String, Object>) targetConfig.get(CONFIG_KEY_TARGET);
        Map<String, Object> lifecycle = (Map<String, Object>) targetObj.get(CONFIG_KEY_LIFECYCLE_HOOKS);
        if (lifecycle == null) {
            lifecycle = new HashMap<>();
            targetObj.put(CONFIG_KEY_LIFECYCLE_HOOKS, lifecycle);
        }
        List<Map<String, Object>> hooks = (List<Map<String, Object>>) lifecycle.get(hookType);
        if (hooks == null) {
            hooks = new LinkedList<>();
            lifecycle.put(hookType, hooks);
        }
        if (hooks.stream().noneMatch(hook -> StringUtils.equals(hookName, hook.get(CONFIG_KEY_HOOK_NAME).toString()))) {
            hooks.add(singletonMap(CONFIG_KEY_HOOK_NAME, hookName));
        } else {
            logger.info("Target '{}' already has lifecycle hook '{}.{}'", target.getId(), hookType, hookName);
        }
    }

}
