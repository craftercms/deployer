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
package org.craftercms.deployer.impl.rest;

/**
 * Common constants for REST controllers.
 *
 * @author avasquez
 */
public class RestConstants {

    /**
     * Environment path variable name.
     */
    public static final String ENV_PATH_VAR_NAME = "env";
    /**
     * Site name path variable name.
     */
    public static final String SITE_NAME_PATH_VAR_NAME = "site_name";
    /**
     * Request param that indicates if request shouldn't finish until the deployment is done.
     */
    public static final String WAIT_TILL_DONE_PARAM_NAME = "wait_till_done";

    private RestConstants() {
    }

}
