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
package org.craftercms.deployer.api.results;

/**
 * Created by alfonsovasquez on 12/15/16.
 */
public abstract class DeploymentResult {

    protected String deploymentId;
    protected boolean success;

    public DeploymentResult(String deploymentId, boolean success) {
        this.deploymentId = deploymentId;
        this.success = success;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public boolean isSuccess() {
        return success;
    }

}
