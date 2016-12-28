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
public class CommonConfigurationProperties {

    private CommonConfigurationProperties() {
    }

    // Main properties

    public static final String DEPLOYMENT_ID_PROPERTY_NAME = "deployment.id";
    public static final String DEPLOYMENT_ROOT_FOLDER_PROPERTY_NAME = "deployment.rootFolder";
    public static final String DEPLOYMENT_PIPELINE_PROPERTY_NAME = "deployment.pipeline";

    // Processor properties

    public static final String PROCESSOR_NAME_PROPERTY_NAME = "processorName";

}
