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
package org.craftercms.deployer.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.craftercms.commons.config.ConfigUtils.getConfigurationsAt;
import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.impl.DeploymentConstants.HOOK_NAME_CONFIG_KEY;

/**
 * Default implementation of {@link TargetLifecycleHooksResolver}.
 *
 * @author avasquez
 */
@Component("targetLifecycleHooksResolver")
public class TargetLifecycleHooksResolverImpl implements TargetLifecycleHooksResolver {

    private static final Logger logger = LoggerFactory.getLogger(TargetLifecycleHooksResolverImpl.class);

    @Override
    public List<TargetLifecycleHook> getHooks(HierarchicalConfiguration<ImmutableNode> configuration,
                                              ApplicationContext applicationContext, String lifecycleHooksPropertyName)
            throws ConfigurationException, DeployerException {
        List<HierarchicalConfiguration<ImmutableNode>> hookConfigs = getConfigurationsAt(configuration,
                                                                                         lifecycleHooksPropertyName);
        List<TargetLifecycleHook> hooks = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(hookConfigs)) {
            for (HierarchicalConfiguration hookConfig : hookConfigs) {
                String hookName = getRequiredStringProperty(hookConfig, HOOK_NAME_CONFIG_KEY);

                logger.debug("Initializing target lifecycle hook '{}'", hookName);

                try {
                    TargetLifecycleHook hook = applicationContext.getBean(hookName, TargetLifecycleHook.class);
                    hook.init(hookConfig);

                    hooks.add(hook);
                } catch (NoSuchBeanDefinitionException e) {
                    throw new DeployerException("No target lifecycle hook bean found with name '" + hookName + "'", e);
                } catch (Exception e) {
                    throw new DeployerException("Failed to initialize target lifecycle hook '" + hookName + "'", e);
                }
            }
        }

        return hooks;
    }

}
