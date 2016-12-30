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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by alfonsovasquez on 12/29/16.
 */
public class Deployment {

    protected String targetId;
    protected volatile Instant start;
    protected volatile Instant end;
    protected volatile boolean running;
    protected volatile Status status;
    protected volatile ChangeSet changeSet;
    protected List<ProcessorExecution> processorExecutions;
    protected Lock statusesLock;

    public Deployment(String targetId) {
        this.targetId = targetId;
        this.start = Instant.now();
        this.running = true;
        this.processorExecutions = new ArrayList<>();
        this.statusesLock = new ReentrantLock();
    }

    public String getTargetId() {
        return targetId;
    }

    public Instant getStart() {
        return start;
    }

    public void setStart(Instant start) {
        this.start = start;
    }

    public Instant getEnd() {
        return end;
    }

    public void setEnd(Instant end) {
        this.end = end;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

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

    public enum Status {
        SUCCESS, FAILURE;
    }

}
