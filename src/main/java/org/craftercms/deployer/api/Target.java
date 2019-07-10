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
package org.craftercms.deployer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.craftercms.deployer.api.exceptions.TargetNotReadyException;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;

/**
 * Represents a deployment target.
 *
 * @author avasquez
 */
public interface Target {

    String AUTHORING_ENV = "authoring";

    enum Status {
        CREATED,
        INIT_IN_PROGRESS,
        INIT_FAILED,
        INIT_COMPLETED,
        DELETE_IN_PROGRESS,
        DELETED
    }

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
     * Returns the status of the target
     */
    @JsonProperty("status")
    Status getStatus();

    /**
     * Indicates if Crafter Search should be used instead of Elasticsearch.
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
    HierarchicalConfiguration<ImmutableNode> getConfiguration();

    /**
     * Returns this target's Spring application context
     */
    @JsonIgnore
    ConfigurableApplicationContext getApplicationContext();

    /**
     * Starts the initialization of the target (asynchronous operation). Called when the create target API is called
     * or the target config is loaded.
     */
    void init();

    /**
     * Starts a new deployment for the target (asynchronous operation if {@code waitTillDone} is false).
     *
     * @param waitTillDone  if the method should wait till the deployment is done or return immediately
     * @param params        miscellaneous parameters that can be used by the processors.
     *
     * @return the deployment info
     * @throws TargetNotReadyException if the target is not in {@link Status#INIT_COMPLETED}
     */
    Deployment deploy(boolean waitTillDone, Map<String, Object> params) throws TargetNotReadyException;

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
    boolean isEnvAuthoring();

    /**
     * Performs a cleanup of the local repository.
     */
    void cleanRepo();

    /**
     * Closes the target, releases any open resources and stops any running threads associated to the target.
     */
    void close();

    /**
     * Deletes the target, executing any delete hooks. Calls {@link #close()} too.
     */
    void delete();

}
