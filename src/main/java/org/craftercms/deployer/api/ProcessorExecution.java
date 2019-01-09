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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;

import static org.craftercms.deployer.api.Deployment.Status;

/**
 * Represents the info of a single processor execution.
 *
 * @author avasquez
 */
public class ProcessorExecution {

    protected String processorName;
    protected volatile ZonedDateTime start;
    protected volatile ZonedDateTime end;
    protected volatile Status status;
    protected volatile Object statusDetails;

    public ProcessorExecution(String processorName) {
        this.processorName = processorName;
        this.start = ZonedDateTime.now();
    }

    @JsonProperty("processor_name")
    public String getProcessorName() {
        return processorName;
    }

    @JsonProperty("start")
    public ZonedDateTime getStart() {
        return start;
    }

    @JsonProperty("end")
    public ZonedDateTime getEnd() {
        return end;
    }

    @JsonProperty("running")
    public boolean isRunning() {
        return end == null;
    }

    @JsonProperty("status")
    public Status getStatus() {
        return status;
    }

    @JsonProperty("status_details")
    public Object getStatusDetails() {
        return statusDetails;
    }

    public void setStatusDetails(Object statusDetails) {
        this.statusDetails = statusDetails;
    }

    public void endExecution(Status status) {
        if (isRunning()) {
            this.end = ZonedDateTime.now();
            this.status = status;
        }
    }

}
