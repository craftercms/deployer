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
package org.craftercms.deployer.utils.core;

import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.TargetResolver;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.impl.TargetImpl;

import static org.craftercms.deployer.api.Target.AUTHORING_ENV;

/**
 * Implementation of {@link TargetResolver} that uses the current {@link Target}
 *
 * @author joseross
 * @since 3.1.6
 */
public class TargetAwarePublishingTargetResolver implements TargetResolver {

    @Override
    public String getTarget() {
        Target target = TargetImpl.getCurrent();
        if (target == null) {
            throw new IllegalStateException("Can't find current target");
        }
        return StringUtils.equals(target.getEnv(), AUTHORING_ENV)? PREVIEW : target.getEnv();
    }

}
