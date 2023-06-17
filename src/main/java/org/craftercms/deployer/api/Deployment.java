/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.craftercms.deployer.api.cluster.ClusterMode;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.Collections.emptyMap;
import static org.craftercms.deployer.impl.DeploymentConstants.DEPLOYMENT_MODE_PARAM_NAME;

/**
 * Represents a deployment. Contains every important status information of a particular deployment execution.
 *
 * @author avasquez
 */
@JsonPropertyOrder({ "mode", "status", "running", "duration", "start", "end", "created_files",
    "updated_files", "deleted_files" })
public class Deployment {

    protected Target target;
    protected volatile ZonedDateTime start;
    protected volatile ZonedDateTime end;
    protected volatile Status status;
    protected volatile ChangeSet changeSet = new ChangeSet();
    protected List<ProcessorExecution> processorExecutions;
    protected Map<String, Object> params;
    protected Lock lock;
    protected Mode mode = Mode.PUBLISH;
    protected ClusterMode clusterMode = ClusterMode.UNKNOWN;

    public Deployment(Target target) {
        this(target, emptyMap());
    }

    public Deployment(Target target, Map<String, Object> params) {
        this.target = target;
        this.processorExecutions = new ArrayList<>();
        this.params = new ConcurrentHashMap<>(params);
        this.lock = new ReentrantLock();
        if (params.containsKey(DEPLOYMENT_MODE_PARAM_NAME)) {
            mode = Mode.valueOf((String) params.get(DEPLOYMENT_MODE_PARAM_NAME));
        }
    }

    /**
     * Returns the {@link Target} being deployed.
     */
    public Target getTarget() {
        return target;
    }

    /**
     * Returns the start date of the deployment.
     */
    @JsonProperty("start")
    public ZonedDateTime getStart() {
        return start;
    }

    /**
     * Returns the end date of the deployment.
     */
    @JsonProperty("end")
    public ZonedDateTime getEnd() {
        return end;
    }

    /**
     * Returns true if the deployment is still running.
     */
    public boolean isRunning() {
        return start != null && end == null;
    }

    /**
     * Returns the duration of the deployment.
     */
    @JsonProperty("duration")
    public Long getDuration() {
        if (start != null && end != null) {
            return start.until(end, ChronoUnit.MILLIS);
        } else {
            return null;
        }
    }

    /**
     * Returns the status of the deployment, either success or failure.
     */
    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    @JsonProperty("mode")
    public Mode getMode() {
        return mode;
    }


    /**
     * Returns the current {@link ClusterMode}
     */
    public ClusterMode getClusterMode() {
        return clusterMode;
    }

    /**
     * Returns the change set of the deployment.
     */
    public ChangeSet getChangeSet() {
        return changeSet;
    }

    /**
     * Sets the change set of the deployment.
     */
    public void setChangeSet(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    /**
     * Returns true if the change set is null or empty.
     */
    @JsonIgnore
    public boolean isChangeSetEmpty() {
        return changeSet == null || changeSet.isEmpty();
    }

    /**
     * Starts the deployment.
     */
    public void start() {
        if (!isRunning()) {
            this.end = null;
            this.start = ZonedDateTime.now();
        }
    }

    /**
     * Ends the deployment with the specified status.
     */
    public void end(Status status) {
        if (isRunning()) {
            this.end = ZonedDateTime.now();
            this.status = status;
        }
    }

    /**
     * Returns the list of {@link ProcessorExecution}s.
     */
    public List<ProcessorExecution> getProcessorExecutions() {
        lock.lock();
        try {
            return new ArrayList<>(processorExecutions);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Adds a {@link ProcessorExecution} to the list.
     */
    public void addProcessorExecution(ProcessorExecution status) {
        lock.lock();
        try {
            processorExecutions.add(status);
        } finally {
            lock.unlock();
        }
    }


    /**
     * Adds a param that can be used by processors during the deployment.
     *
     * @param name  the name of the param
     * @param value the value of the param
     */
    public void addParam(String name, Object value) {
        params.put(name, value);
    }

    /**
     * Returns a param that can be used by processors during the deployment.
     *
     * @param name  the name of the param
     *
     * @return the value of the param
     */
    public Object getParam(String name) {
        return params.get(name);
    }

    /**
     * Removes the specified param
     *
     * @param name the name of the param
     */
    public void removeParam(String name) {
        params.remove(name);
    }

    /**
     * Sets the current {@link ClusterMode} for this deployment
     */
    public void setClusterMode(final ClusterMode clusterMode) {
        this.clusterMode = clusterMode;
    }

    public enum Status {
        SUCCESS, FAILURE, INTERRUPTED
    }

    public enum Mode {
        PUBLISH, SEARCH_INDEX
    }

    @Override
    public String toString() {
        return "Deployment{" +
               "targetId='" + target.getId() + "'" +
               ", start=" + start +
               ", end=" + end +
               ", running=" + isRunning() +
               ", duration=" + getDuration() +
               ", status=" + status +
               ", mode=" + mode +
               ", changeSet=" + changeSet +
               ", processorExecutions=" + processorExecutions +
               ", params=" + params + '}';
    }
}
