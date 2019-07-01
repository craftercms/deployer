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

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.springframework.context.ApplicationContext;

import java.util.List;

/**
 * Resolver that uses target-specific YAML configuration and Spring configuration to retrieve the
 * {@link TargetLifecycleHook}s for a target.
 *
 * @author avasquez
 */
public interface TargetLifecycleHooksResolver {

    /**
     * Resolved a {@link TargetLifecycleHook} based on the specified configuration
     *
     * @param configuration                 the target's YAML configuration
     * @param applicationContext            the target's application context
     * @param lifecycleHooksPropertyName    the name of the hook list property in the YAML configuration
     *
     * @return the lifecycle hooks
     *
     * @throws ConfigurationException if a configuration related exception occurs
     * @throws DeployerException if a general error occurs
     */
    List<TargetLifecycleHook> getHooks(HierarchicalConfiguration<ImmutableNode> configuration,
                                       ApplicationContext applicationContext, String lifecycleHooksPropertyName)
            throws ConfigurationException, DeployerException;

}
