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
 *
 *
 * @author joseross
 * @since 4.0
 */
public abstract class ConditionalEnvUpgradeOperation extends AbstractTargetUpgradeOperation {

    public static final String CONFIG_KEY_ENV_PATTERN = "envPattern";

    protected String envPattern;

    @Override
    public void init(String currentVersion, String nextVersion, HierarchicalConfiguration config)
            throws ConfigurationException {
        envPattern = getRequiredStringProperty(config, CONFIG_KEY_ENV_PATTERN);

        super.init(currentVersion, nextVersion, config);
    }

    @Override
    protected void doExecute(Target target, Map<String, Object> targetConfig) throws Exception {
        if (target.getEnv().matches(envPattern)) {
            doExecuteInternal(target, targetConfig);
        }
    }

    protected abstract void doExecuteInternal(Target target, Map<String, Object> targetConfig);

}
