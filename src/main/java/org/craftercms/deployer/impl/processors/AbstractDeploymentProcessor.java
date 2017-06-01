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
package org.craftercms.deployer.impl.processors;

import org.craftercms.deployer.api.DeploymentProcessor;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;

/**
 * Base class for all {@link DeploymentProcessor}s. All processor are expected to be configured as prototypes in Spring, so it's
 * possible to have processor instances per target, and inject target specific properties.
 *
 * @author avasquez
 */
public abstract class AbstractDeploymentProcessor implements DeploymentProcessor, BeanNameAware  {

    protected String env;
    protected String siteName;
    protected String targetId;
    protected String name;

    /**
     * Sets the environment of the site.
     */
    @Required
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * Sets the site name.
     */
    @Required
    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    /**
     * Sets the target ID.
     */
    @Required
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    /**
     * Sets the bean name of the processor.
     */
    @Override
    public void setBeanName(String name) {
        this.name = name;
    }

}
