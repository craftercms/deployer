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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.SiteContext;
import org.craftercms.deployer.api.SiteResolver;
import org.craftercms.deployer.api.event.ErrorEvent;
import org.craftercms.deployer.api.event.PostDeployEvent;
import org.craftercms.deployer.api.result.DeploymentFailure;
import org.craftercms.deployer.api.result.DeploymentResult;
import org.craftercms.deployer.api.result.DeploymentSuccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by alfonsovasquez on 30/11/16.
 */
@Component("deploymentService")
public class DeploymentServiceImpl implements DeploymentService {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentServiceImpl.class);

    @Autowired
    protected SiteResolver siteResolver;

    @Override
    public  List<DeploymentResult> deployAllSites() {
        List<SiteContext> siteContexts = siteResolver.resolveAll();
        List<DeploymentResult> results = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(siteContexts)) {
            siteContexts.forEach(context -> deploySite(context, results));
        }

        return results;
    }

    protected void deploySite(SiteContext siteContext, List<DeploymentResult> results) {
        try {
            ChangeSet changeSet = siteContext.getDeployer().deploy();

            siteContext.fireEvent(new PostDeployEvent(siteContext, changeSet));

            registerDeploymentSuccess(siteContext.getName(), results);
        } catch (Exception e) {
            siteContext.fireEvent(new ErrorEvent(siteContext, e));

            registerDeploymentFailure(siteContext.getName(), e, results);
        }
    }

    protected void registerDeploymentSuccess(String siteName, List<DeploymentResult> results) {
        results.add(new DeploymentSuccess(siteName));

        logger.info("Deployment of site '{}' successful", siteName);
    }

    protected void registerDeploymentFailure(String siteName, Exception exception, List<DeploymentResult> results) {
        results.add(new DeploymentFailure(siteName, exception.toString()));

        logger.error("Deployment of site '" + siteName + "' failed", exception);
    }

}
