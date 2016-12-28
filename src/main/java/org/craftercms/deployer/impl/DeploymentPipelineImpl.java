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

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeploymentException;

/**
 * Created by alfonsovasquez on 12/18/16.
 */
public class DeploymentPipelineImpl implements DeploymentPipeline {

    protected List<DeploymentProcessor> processors;

    public DeploymentPipelineImpl(List<DeploymentProcessor> processors) {
        this.processors = processors;
    }

    @Override
    public ChangeSet execute(DeploymentContext context) throws DeploymentException {
        ChangeSet changeSet = new ChangeSetImpl();

        if (CollectionUtils.isNotEmpty(processors)) {
            for (DeploymentProcessor processor : processors) {
                changeSet = processor.execute(context, changeSet);
            }
        }

        return changeSet;
    }

    @Override
    public void destroy() throws DeploymentException {
        if (CollectionUtils.isNotEmpty(processors)) {
            for (DeploymentProcessor processor : processors) {
                processor.destroy();
            }
        }
    }

}
