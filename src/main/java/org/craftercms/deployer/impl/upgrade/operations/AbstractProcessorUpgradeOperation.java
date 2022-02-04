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

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Abstract extension of {@link ConditionalEnvUpgradeOperation} used for all upgrades related to a single processor
 *
 * @author joseross
 * @since 4.0.0
 */
public abstract class AbstractProcessorUpgradeOperation extends ConditionalEnvUpgradeOperation {

    /**
     * The name of the processor to update
     */
    protected String processorName;

    @Override
    public void init(String currentVersion, String nextVersion, HierarchicalConfiguration config)
            throws ConfigurationException {
        processorName = getRequiredStringProperty(config, CONFIG_KEY_PROCESSOR);

        super.init(currentVersion, nextVersion, config);
    }

}
