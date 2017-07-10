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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.ArrayUtils;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.DeploymentConstants;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link org.craftercms.deployer.api.DeploymentProcessor}s that are executed during the main deployment phase, which is
 * the phase where the change set is retrieved and the files are processed. Inclusion/exclusion of files is handled by this class, through
 * YAML configuration properties {@code includeFiles} and {@code excludeFiles}. So basically each processor instance can have its own
 * set of inclusions/exclusions.
 *
 * @author avasquez
 */
public abstract class AbstractMainDeploymentProcessor extends AbstractDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(AbstractMainDeploymentProcessor.class);

    protected String[] includeFiles;
    protected String[] excludeFiles;

    @Override
    public void init(Configuration config) throws DeployerException {
        includeFiles = ConfigUtils.getStringArrayProperty(config, DeploymentConstants.PROCESSOR_INCLUDE_FILES_CONFIG_KEY);
        excludeFiles = ConfigUtils.getStringArrayProperty(config, DeploymentConstants.PROCESSOR_EXCLUDE_FILES_CONFIG_KEY);

        doInit(config);
    }

    @Override
    public void execute(Deployment deployment) {
        ChangeSet filteredChangeSet = getFilteredChangeSet(deployment.getChangeSet());
        
        if (shouldExecute(deployment, filteredChangeSet)) {
            ProcessorExecution execution = new ProcessorExecution(name);

            deployment.addProcessorExecution(execution);

            try {
                logger.info("----- {} @ {} -----", name, targetId);

                ChangeSet processedChangeSet = doExecute(deployment, execution, filteredChangeSet);
                if (processedChangeSet != null) {
                    deployment.setChangeSet(processedChangeSet);
                }

                execution.endExecution(Deployment.Status.SUCCESS);
            } catch (Exception e) {
                logger.error("Processor '" + name + "' for target '" + targetId + "' failed", e);

                execution.setStatusDetails(e.toString());
                execution.endExecution(Deployment.Status.FAILURE);

                if (failDeploymentOnProcessorFailure()) {
                    deployment.end(Deployment.Status.FAILURE);
                }
            }
        }
    }

    protected ChangeSet getFilteredChangeSet(ChangeSet changeSet) {
        if (changeSet != null && (ArrayUtils.isNotEmpty(includeFiles) || ArrayUtils.isNotEmpty(excludeFiles))) {
            List<String> matchedCreatedFiles = new ArrayList<>();
            List<String> matchedUpdatedFiles = new ArrayList<>();
            List<String> matchedDeletedFiles = new ArrayList<>();

            for (String path : changeSet.getCreatedFiles()) {
                if (shouldIncludeFile(path)) {
                    matchedCreatedFiles.add(path);
                }
            }
            for (String path : changeSet.getUpdatedFiles()) {
                if (shouldIncludeFile(path)) {
                    matchedUpdatedFiles.add(path);
                }
            }
            for (String path : changeSet.getDeletedFiles()) {
                if (shouldIncludeFile(path)) {
                    matchedDeletedFiles.add(path);
                }
            }

            return new ChangeSet(matchedCreatedFiles, matchedUpdatedFiles, matchedDeletedFiles);
        } else {
            return changeSet;
        }
    }

    protected boolean shouldIncludeFile(String file) {
        return (ArrayUtils.isEmpty(includeFiles) || RegexUtils.matchesAny(file, includeFiles)) &&
               (ArrayUtils.isEmpty(excludeFiles) || !RegexUtils.matchesAny(file, excludeFiles));
    }

    protected boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet) {
        // Run if the deployment is running and change set is not empty
        return deployment.isRunning() && filteredChangeSet != null && !filteredChangeSet.isEmpty();
    }

    protected abstract void doInit(Configuration config) throws DeployerException;

    protected abstract ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                           ChangeSet filteredChangeSet) throws DeployerException;

    protected abstract boolean failDeploymentOnProcessorFailure();

}
