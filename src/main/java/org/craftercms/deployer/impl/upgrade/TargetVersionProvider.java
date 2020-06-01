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

import org.craftercms.commons.upgrade.impl.providers.YamlFileVersionProvider;
import org.craftercms.deployer.impl.TargetImpl;

import java.nio.file.Path;

/**
 * Extension of {@link YamlFileVersionProvider} to support {@link TargetImpl} objects
 */
public class TargetVersionProvider extends YamlFileVersionProvider {

    @Override
    protected Path getFile(Object target) {
        if (!(target instanceof TargetImpl)) {
            throw new IllegalArgumentException("The object is not a valid target");
        }
        return ((TargetImpl) target).getConfigurationFile().toPath();
    }

}
