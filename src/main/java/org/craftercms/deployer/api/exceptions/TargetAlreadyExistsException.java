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
package org.craftercms.deployer.api.exceptions;

/**
 * Exception thrown when a target is about to be created but a target with the same ID already exists.
 *
 * @author avasquez
 */
public class TargetAlreadyExistsException extends DeployerException {

    protected String id;
    protected String env;
    protected String siteName;

    public TargetAlreadyExistsException(String id, String env, String siteName) {
        super("Target '" + id + "' already exists");

        this.id = id;
        this.env = env;
        this.siteName = siteName;
    }

    public String getId() {
        return id;
    }

    public String getEnv() {
        return env;
    }

    public String getSiteName() {
        return siteName;
    }

}
