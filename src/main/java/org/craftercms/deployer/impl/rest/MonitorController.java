package org.craftercms.deployer.impl.rest;

import org.craftercms.commons.monitoring.*;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Simple Rest Controller that monitors Deployer.
 * @author Carlos Ortiz.
 */
@RestController
@RequestMapping(MonitorController.BASE_URL)
public class MonitorController {

     static final String BASE_URL = "/api/1/monitor/";
     private static final String MEMORY_TARGET_URL = "/memory";
    private static final String  STATUS_TARGET_URL = "/status";
    private static final String VERSION_TARGET_URL = "/version";

    /**
     * Uses Crafter Commons Memory Monitor POJO to get current JVM Memory stats.
     * @return {link {@link org.craftercms.commons.monitoring.Memory}}
     */
    @RequestMapping(value = MEMORY_TARGET_URL, method = RequestMethod.GET)
    public ResponseEntity<List<MemoryMonitor>> memoryStats() {
        return new ResponseEntity<>(MemoryMonitor.getMemoryStats(), HttpStatus.OK);
    }

    /**
     * Uses Crafter Commons Status Monitor POJO to get current System status.
     * @return {link {@link org.craftercms.commons.monitoring.Status}}
     */
    @RequestMapping(value = STATUS_TARGET_URL, method = RequestMethod.GET)
    public ResponseEntity<StatusMonitor> status() {
        return new ResponseEntity<>(StatusMonitor.getCurrentStatus(), HttpStatus.OK);
    }


    /**
     * Uses Crafter Commons Status Version POJO to get current Deployment and JVM runtime and version information.
     * @return {link {@link org.craftercms.commons.monitoring.Version}}
     */
    @RequestMapping(value = VERSION_TARGET_URL, method = RequestMethod.GET)
    public ResponseEntity<VersionMonitor> version() throws Exception {
        try {
            return new ResponseEntity<>(VersionMonitor.getVersion(this.getClass()), HttpStatus.OK);
        }catch (IOException ex){
            LoggerFactory.getLogger(MonitorController.class).error("Unable to read manifest file",ex);
           throw new Exception("Unable to read Manifest File");

        }
    }

}