/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.deployer.impl;

/**
 * Created by alfonsovasquez on 12/26/16.
 */
public class CommonConfigurationKeys {

    private CommonConfigurationKeys() {
    }

    // Global Properties

    // Target Specific Properties

    public static final String TARGET_ID_CONFIG_KEY = "target.id";
    public static final String TARGET_ROOT_FOLDER_PATH_CONFIG_KEY = "target.rootFolderPath";
    public static final String DEPLOYMENT_PIPELINE_CONFIG_KEY = "target.deployment.pipeline";
    public static final String POST_DEPLOYMENT_PIPELINE_CONFIG_KEY = "target.postDeployment.pipeline";

    // Processor Properties

    public static final String PROCESSOR_NAME_CONFIG_KEY = "processorName";

}
