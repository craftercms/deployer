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

package org.craftercms.deployer.impl.upgrade;

import java.util.Collections;
import java.util.List;

import org.craftercms.commons.upgrade.UpgradePipelineFactory;
import org.craftercms.commons.upgrade.impl.AbstractUpgradeManager;
import org.craftercms.deployer.api.Target;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Implementation of {@link org.craftercms.commons.upgrade.UpgradeManager} for Crafter Deployer
 *
 * @author joseross
 * @since 3.1.5
 */
@Component
public class DeployerUpgradeManager extends AbstractUpgradeManager<Target> {

    protected UpgradePipelineFactory<Target> targetPipelineFactory;

    @Autowired
    public DeployerUpgradeManager(final UpgradePipelineFactory<Target> targetPipelineFactory) {
        this.targetPipelineFactory = targetPipelineFactory;
    }

    @Override
    protected List<Target> doGetTargets() {
        // Returns an empty list because all targets are upgraded when loaded, no extra steps needed
        return Collections.emptyList();
    }

    @Override
    protected void doUpgrade(final Target target) throws Exception {
        executePipeline(target, targetPipelineFactory);
    }

}
