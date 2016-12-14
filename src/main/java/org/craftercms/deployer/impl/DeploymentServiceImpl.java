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
package org.craftercms.deployer.impl;

import java.util.List;

import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.SiteContext;
import org.craftercms.deployer.api.SiteResolver;
import org.craftercms.deployer.api.event.ErrorEvent;
import org.craftercms.deployer.api.event.PostDeployEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by alfonsovasquez on 30/11/16.
 */
@Component("deploymentService")
public class DeploymentServiceImpl implements DeploymentService {

    @Autowired
    protected SiteResolver siteResolver;

    @Override
    public void deployAllSites() {
        List<SiteContext> siteContexts = siteResolver.resolveAll();

        siteContexts.forEach(this::deploySite);
    }

    protected void deploySite(SiteContext siteContext) {
        try {
            ChangeSet changeSet = siteContext.getDeployer().deploy();

            siteContext.fireEvent(new PostDeployEvent(siteContext, changeSet));
        } catch (Exception e) {
            siteContext.fireEvent(new ErrorEvent(siteContext, e));
        }
    }

}
