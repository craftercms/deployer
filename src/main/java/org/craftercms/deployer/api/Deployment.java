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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a deployment. Contains every important status information of a particular deployment execution.
 *
 * @author avasquez
 */
public class Deployment {

    protected Target target;
    protected volatile ZonedDateTime start;
    protected volatile ZonedDateTime end;
    protected volatile long duration;
    protected volatile Status status;
    protected volatile ChangeSet changeSet;
    protected List<ProcessorExecution> processorExecutions;
    protected Lock statusesLock;
    protected Map<String, Object> attributes;

    public Deployment(Target target) {
        this.target = target;
        this.start = ZonedDateTime.now();
        this.processorExecutions = new ArrayList<>();
        this.statusesLock = new ReentrantLock();
        this.attributes = new ConcurrentHashMap<>();
    }

    /**
     * Returns the {@link Target} being deployed.
     */
    @JsonProperty("target")
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
     * Returns the duration of the deployment.
     */
    @JsonProperty("duration")
    public long getDuration() {
        return duration;
    }

    /**
     * Returns true if the deployment is still running.
     */
    @JsonProperty("running")
    public boolean isRunning() {
        return end == null;
    }

    /**
     * Returns the status of the deployment, either success or failure.
     */
    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    /**
     * Returns the change set of the deployment.
     */
    @JsonProperty("change_set")
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
     * Ends the deployment with the specified status.
     */
    public void endDeployment(Status status) {
        if (isRunning()) {
            this.end = ZonedDateTime.now();
            this.status = status;
            this.duration = start.until(end, ChronoUnit.MILLIS);
        }
    }

    /**
     * Returns the list of {@link ProcessorExecution}s.
     */
    @JsonProperty("processor_executions")
    public List<ProcessorExecution> getProcessorExecutions() {
        statusesLock.lock();
        try {
            return new ArrayList<>(processorExecutions);
        } finally {
            statusesLock.unlock();
        }
    }

    /**
     * Adds a {@link ProcessorExecution} to the list.
     */
    public void addProcessorExecution(ProcessorExecution status) {
        statusesLock.lock();
        try {
            processorExecutions.add(status);
        } finally {
            statusesLock.unlock();
        }
    }

    /**
     * Adds a miscellaneous attribute that can be used by processors during the deployment.
     *
     * @param name  the name of the attribute
     * @param value the value of the attribute
     */
    public void addAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    /**
     * Returns a miscellaneous attribute that can be used by processors during the deployment.
     *
     * @param name  the name of the attribute
     * @return the value of the attribute
     */
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public enum Status {
        SUCCESS, FAILURE
    }

}
