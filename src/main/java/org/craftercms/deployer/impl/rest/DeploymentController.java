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
package org.craftercms.deployer.impl.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.DeploymentResolver;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.results.DeploymentResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by alfonsovasquez on 12/14/16.
 */
@RestController
@RequestMapping("/api/1/deployment")
public class DeploymentController {

    protected final DeploymentService deploymentService;
    protected final DeploymentResolver deploymentResolver;

    @Autowired
    public DeploymentController(DeploymentService deploymentService, DeploymentResolver deploymentResolver) {
        this.deploymentService = deploymentService;
        this.deploymentResolver = deploymentResolver;
    }

    @RequestMapping("/list/all")
    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> deploymentInfoList = new ArrayList<>();
        List<DeploymentContext> deploymentContexts = deploymentResolver.resolveAll();

        if (CollectionUtils.isNotEmpty(deploymentContexts)) {
            deploymentContexts.forEach(context -> {
                Date dateCreated = new Date(context.getDateCreated());

                Map<String, Object> deploymentInfo = new HashMap<>(2);
                deploymentInfo.put("id", context.getId());
                deploymentInfo.put("dateCreated", DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(dateCreated));

                deploymentInfoList.add(deploymentInfo);
            });
        }

        return deploymentInfoList;
    }

    @RequestMapping("/deploy/all")
    public List<DeploymentResult> deployAll() {
        return deploymentService.deployAllSites();
    }

}
