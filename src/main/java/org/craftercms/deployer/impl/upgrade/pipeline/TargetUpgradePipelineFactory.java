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
package org.craftercms.deployer.impl.upgrade.pipeline;

import org.craftercms.commons.upgrade.UpgradeOperation;
import org.craftercms.commons.upgrade.UpgradePipeline;
import org.craftercms.commons.upgrade.VersionProvider;
import org.craftercms.commons.upgrade.impl.pipeline.DefaultUpgradePipelineFactoryImpl;
import org.craftercms.deployer.api.Target;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Extension of {@link DefaultUpgradePipelineFactoryImpl} that creates instances of {@link TargetUpgradePipeline}
 *
 * @author joseross
 * @since 4.0.0
 */
public class TargetUpgradePipelineFactory extends DefaultUpgradePipelineFactoryImpl<Target> {

    public TargetUpgradePipelineFactory(String pipelineName, Resource configurationFile,
                                        VersionProvider<Target> versionProvider) {
        super(pipelineName, configurationFile, versionProvider);
    }

    @Override
    protected UpgradePipeline<Target> createPipeline(String name, List<UpgradeOperation<Target>> upgradeOperations) {
        return new TargetUpgradePipeline(name, upgradeOperations);
    }

}
