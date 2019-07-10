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
package org.craftercms.deployer.utils.beans;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;

/**
 * Interface implemented by beans that can be initialized through a {@code Configuration} object.
 *
 * @author avasquez
 */
public interface InitializableByConfigBean {

    /**
     * Initializes the bean using the specified configuration.
     *
     * @param config the bean's configuration
     *
     * @throws ConfigurationException if there's configuration related exception
     * @throws DeployerException if there's a general exception on init
     */
    void init(Configuration config) throws ConfigurationException, DeployerException;

}
