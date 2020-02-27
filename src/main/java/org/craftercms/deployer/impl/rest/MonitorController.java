/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.deployer.impl.rest;

import org.craftercms.commons.monitoring.rest.MonitoringRestControllerBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple Rest Controller that monitors Deployer.
 * @author Carlos Ortiz.
 */
@RestController
@RequestMapping(MonitorController.BASE_URL)
public class MonitorController extends MonitoringRestControllerBase {

    /**
     * Base ULR for monitoring services.
     */
    public static final String BASE_URL = "/api/1";

    @Value("${deployer.main.management.authorizationToken}")
    private String configuredToken;

    @Override
    protected String getConfiguredToken() {
        return configuredToken;
    }
}
