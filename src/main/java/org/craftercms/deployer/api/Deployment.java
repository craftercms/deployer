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

    @JsonProperty("target")
    public Target getTarget() {
        return target;
    }

    @JsonProperty("start")
    public ZonedDateTime getStart() {
        return start;
    }

    @JsonProperty("end")
    public ZonedDateTime getEnd() {
        return end;
    }

    @JsonProperty("duration")
    public long getDuration() {
        return duration;
    }

    @JsonProperty("running")
    public boolean isRunning() {
        return end == null;
    }

    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    @JsonProperty("change_set")
    public ChangeSet getChangeSet() {
        return changeSet;
    }

    public void setChangeSet(ChangeSet changeSet) {
        this.changeSet = changeSet;
    }

    @JsonIgnore
    public boolean isChangeSetEmpty() {
        return changeSet == null || changeSet.isEmpty();
    }

    public void endDeployment(Status status) {
        if (isRunning()) {
            this.end = ZonedDateTime.now();
            this.status = status;
            this.duration = start.until(end, ChronoUnit.MILLIS);
        }
    }

    @JsonProperty("processor_executions")
    public List<ProcessorExecution> getProcessorExecutions() {
        statusesLock.lock();
        try {
            return new ArrayList<>(processorExecutions);
        } finally {
            statusesLock.unlock();
        }
    }

    public void addProcessorExecution(ProcessorExecution status) {
        statusesLock.lock();
        try {
            processorExecutions.add(status);
        } finally {
            statusesLock.unlock();
        }
    }

    @JsonIgnore
    public Map<String, Object> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    public void addAttribute(String name, Object object) {
        attributes.put(name, object);
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public enum Status {
        SUCCESS, FAILURE
    }

}
