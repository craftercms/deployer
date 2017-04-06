package org.craftercms.deployer.impl.rest;

import org.apache.commons.io.FileUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Simple Rest Controller that monitors Deployer.
 * @author Carlos Ortiz.
 */
@RestController
@RequestMapping(MonitorController.BASE_URL)
public class MonitorController {

     static final String BASE_URL = "/api/1/monitoring";
     private static final String MONITOR_TARGET_URL = "/status";

    /**
     * Uses Java Management Service to get Os,Uptime and memory (heap) usage.
     * @return A map with OS version, Heap Memory usage, Current Date (in ms), and Uptime in Hours/Minutes/Seconds format.
     */
    @RequestMapping(value = MONITOR_TARGET_URL, method = RequestMethod.GET)
    public ResponseEntity<Map<String, String>> status() {
        Map<String, String> result = new HashMap<>();
        MemoryMXBean memoryManagerMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMem = memoryManagerMXBean.getHeapMemoryUsage();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
        result.put("OS", String.format("%s-%s %s", os.getName(), os.getArch(), os.getVersion()));
        result.put("Uptime", String.format("%sh %sm %ss", TimeUnit.MILLISECONDS.toHours(uptime), TimeUnit.MILLISECONDS.toMinutes(uptime),
                TimeUnit.MILLISECONDS.toSeconds(uptime)));
        result.put("Heap Mem", String.format("%s of %s", FileUtils.byteCountToDisplaySize(heapMem.getUsed()),
                FileUtils.byteCountToDisplaySize(heapMem.getMax())));
        result.put("Now",String.valueOf(new Date()));
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}