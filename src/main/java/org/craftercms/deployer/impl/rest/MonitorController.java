/*
 *   Copyright (C) 2007-2017 Crafter Software Corporation.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.craftercms.deployer.impl.rest;

import java.io.IOException;
import java.util.List;

import org.craftercms.commons.monitoring.MemoryMonitor;
import org.craftercms.commons.monitoring.StatusMonitor;
import org.craftercms.commons.monitoring.VersionMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple Rest Controller that monitors Deployer.
 * @author Carlos Ortiz.
 */
@RestController
@RequestMapping(MonitorController.BASE_URL)
public class MonitorController {

    /**
     * Base ULR for monitoring services.
     */
    public static final String BASE_URL = "/api/1/monitor/";
    /**
     * Memory service URL.
     */
    private static final String MEMORY_URL = "/memory";
    /**
     * Status service URL.
     */
    private static final String STATUS_URL = "/status";
    /**
     * Version service URL.
     */
    private static final String VERSION_URL = "/version";
    /**
     * Class LOGGER.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorController.class);


    /**
     * Empty like my soul.
     */
    public MonitorController() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    /**
     * Uses Crafter Commons Memory Monitor POJO to get current JVM Memory stats.
     * @return {link {@link MemoryMonitor}}
     */
    @RequestMapping(value = MEMORY_URL, method = RequestMethod.GET)
    public ResponseEntity<List<MemoryMonitor>> memoryStats() {
        return new ResponseEntity<>(MemoryMonitor.getMemoryStats(), HttpStatus.OK);
    }

    /**
     * Uses Crafter Commons Status Monitor POJO to get current System status.
     * @return {link {@link StatusMonitor}}
     */
    @RequestMapping(value = STATUS_URL, method = RequestMethod.GET)
    public ResponseEntity<StatusMonitor> status() {
        return new ResponseEntity<>(StatusMonitor.getCurrentStatus(), HttpStatus.OK);
    }

    /**
     * Uses Crafter Commons Status Version POJO to get current Deployment
     * and JVM runtime and version information.
     *
     * @return {link {@link VersionMonitor}}
     * @throws IOException If Manifest File can't be read.
     */
    @RequestMapping(value = VERSION_URL, method = RequestMethod.GET)
    public ResponseEntity<VersionMonitor> version() throws IOException {
        try {
            final VersionMonitor monitor = VersionMonitor.getVersion(MonitorController.class);
            return new ResponseEntity<>(monitor, HttpStatus.OK);
        } catch (IOException ex) {
            LOGGER.error("Unable to read manifest file", ex);
            throw new IOException("Unable to read manifest file", ex);
        }
    }
}
