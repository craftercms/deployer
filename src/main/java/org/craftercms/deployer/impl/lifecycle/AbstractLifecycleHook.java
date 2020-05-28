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
package org.craftercms.deployer.impl.lifecycle;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base implementation of {@link TargetLifecycleHook}
 *
 * @author joseross
 * @since 3.1.8
 */
public abstract class AbstractLifecycleHook implements TargetLifecycleHook {

    public static final String CONFIG_KEY_DISABLED = "disabled";

    protected Logger logger = LoggerFactory.getLogger(getClass());

    protected boolean disabled;

    @Override
    public void init(Configuration config) throws ConfigurationException, DeployerException {
        disabled = config.getBoolean(CONFIG_KEY_DISABLED, false);
        doInit(config);
    }

    protected abstract void doInit(Configuration config) throws ConfigurationException, DeployerException;

    @Override
    public void execute(Target target) throws DeployerException {
        if (disabled) {
            logger.info("Skipping execution for target {}", target.getId());
        } else {
            doExecute(target);
        }
    }

    protected abstract void doExecute(Target target) throws DeployerException;

}
