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
package org.craftercms.deployer.api.exceptions;

import org.springframework.core.NestedExceptionUtils;

/**
 * Root exception for the Crafter Deployer application.
 *
 * @author avasquez
 */
public class DeployerException extends Exception {

    public DeployerException() {
    }

    public DeployerException(Throwable cause) {
        super(cause);
    }

    public DeployerException(String message) {
        super(message);
    }

    public DeployerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Return the detail message, including the message from the nested exception if there is one.
     */
    @Override
    public String getMessage() {
        return NestedExceptionUtils.buildMessage(super.getMessage(), getCause());
    }

}
