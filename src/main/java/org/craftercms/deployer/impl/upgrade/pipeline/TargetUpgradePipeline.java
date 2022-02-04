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
import org.craftercms.commons.upgrade.exception.UpgradeException;
import org.craftercms.commons.upgrade.impl.UpgradeContext;
import org.craftercms.commons.upgrade.impl.pipeline.DefaultUpgradePipelineImpl;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.impl.upgrade.TargetUpgradeContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Extension of {@link DefaultUpgradePipelineImpl} that creates a backup of the target's configuration file before
 * it is upgraded
 *
 * @author joseross
 * @since 4.0.0
 */
public class TargetUpgradePipeline extends DefaultUpgradePipelineImpl<Target> {

    public TargetUpgradePipeline(String name, List<UpgradeOperation<Target>> upgradeOperations) {
        super(name, upgradeOperations);
    }

    @Override
    public void execute(UpgradeContext<Target> context) throws UpgradeException {
        try {
            if (!isEmpty()) {
                createConfigurationBackup((TargetUpgradeContext) context);
            }
        } catch (Exception e) {
            throw new UpgradeException("Error creating configuration backup for target " +
                    context.getTarget().getId());
        }

        super.execute(context);
    }

    protected void createConfigurationBackup(TargetUpgradeContext context) throws IOException {
        Path configurationFile = context.getTarget().getConfigurationFile().toPath();
        String configurationPath = configurationFile.toAbsolutePath().toString();
        String backupPath = configurationPath + "." +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'.'A")) + ".backup";

        Files.copy(configurationFile, Paths.get(backupPath));
    }

}
