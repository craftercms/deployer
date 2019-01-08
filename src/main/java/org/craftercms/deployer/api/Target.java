/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
import java.util.Collection;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.springframework.scheduling.TaskScheduler;

/**
 * Represents a deployment target.
 *
 * @author avasquez
 */
public interface Target {

    String AUTHORING_ENV = "authoring";

    /**
     * Returns the ID of the target.
     */
    @JsonProperty("id")
    String getId();

    /**
     * Returns the environment of the target.
     */
    @JsonProperty("env")
    String getEnv();

    /**
     * Returns the site name of the target.
     */
    @JsonProperty("site_name")
    String getSiteName();

    /**
     * Returns the load date of the target.
     */
    @JsonProperty("load_date")
    ZonedDateTime getLoadDate();

    /**
     * Indicates if Crafter Search should be used instead of ElasticSearch.
     */
    @JsonProperty("crafter_search_enabled")
    boolean isCrafterSearchEnabled();

    /**
     * Returns the YAML configuration file of the target.
     */
    @JsonIgnore
    File getConfigurationFile();

    /**
     * Returns the configuration of the target.
     */
    @JsonIgnore
    Configuration getConfiguration();

    /**
     * Deploys the target.
     *
     * @param waitTillDone  if the method should wait till the deployment is done or return immediately
     * @param params        miscellaneous parameters that can be used by the processors.
     *
     * @return the deployment info
     */
    Deployment deploy(boolean waitTillDone, Map<String, Object> params);

    /**
     * Schedules deployment of the target.
     *
     * @param scheduler         the scheduler to use
     * @param cronExpression    the cron expression
     */
    void scheduleDeployment(TaskScheduler scheduler, String cronExpression);

    /**
     * Returns the pending deployments.
     */
    @JsonIgnore
    Collection<Deployment> getPendingDeployments();

    /**
     * Returns the current deployment.
     */
    @JsonIgnore
    Deployment getCurrentDeployment();

    /**
     * Returns all deployments (pending and current).
     */
    @JsonIgnore
    Collection<Deployment> getAllDeployments();

    /**
     * Indicates if the target is for authoring environment.
     */
    @JsonIgnore
    boolean isAuthoring();

    /**
     * Performs a cleanup of the local repository.
     */
    void cleanup();

    /**
     * Closes the target and releases any open resources.
     */
    void close();

    /**
     * Creates all search indices needed for the target
     * @throws TargetServiceException if there is any exception creating the indices
     */
    void createIndex() throws TargetServiceException;

    /**
     * Deletes all search indices used for the target
     * @throws TargetServiceException
     */
    void deleteIndex() throws TargetServiceException;

}
