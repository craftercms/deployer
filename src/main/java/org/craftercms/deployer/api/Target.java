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
package org.craftercms.deployer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.springframework.scheduling.TaskScheduler;

/**
 * Represents a deployment target.
 *
 * @author avasquez
 */
public interface Target {

    @JsonProperty("id")
    String getId();

    @JsonProperty("env")
    String getEnv();

    @JsonProperty("site_name")
    String getSiteName();

    @JsonProperty("load_date")
    ZonedDateTime getLoadDate();

    @JsonIgnore
    File getConfigurationFile();

    @JsonIgnore
    Configuration getConfiguration();
    
    Deployment deploy(Map<String, Object> params);

    void scheduleDeployment(TaskScheduler scheduler, String cronExpression);

    void close();

}
