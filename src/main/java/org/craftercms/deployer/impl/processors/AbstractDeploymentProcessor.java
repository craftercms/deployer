/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.lang.RegexUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentProcessor;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.DeploymentConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.annotation.Required;

import java.util.ArrayList;
import java.util.List;

import static org.craftercms.commons.config.ConfigUtils.getBooleanProperty;
import static org.craftercms.commons.config.ConfigUtils.getStringProperty;
import static org.craftercms.deployer.utils.ConfigUtils.getStringArrayProperty;

/**
 * Base class for {@link org.craftercms.deployer.api.DeploymentProcessor}s. Inclusion/exclusion of files is handled
 * by this class, through YAML configuration properties {@code includeFiles} and {@code excludeFiles}. So basically
 * each processor instance can have its own set of inclusions/exclusions.
 *
 * <p>
 * This class also handles processor "jumping", which is triggered when a processor explicitly indicates that after
 * a successful execution the pipeline should skip directly to executing a processor with a certain label.
 * </p>
 *
 * <p>
 * It is also possible to make sure a processor is always executed even if the current {@code ChangeSet} is empty, this
 * can be accomplished with the {@code alwaysRun} property in the YAML configuration.
 * </p>
 *
 * @author avasquez
 */
public abstract class AbstractDeploymentProcessor implements DeploymentProcessor, BeanNameAware {

    private static final Logger logger = LoggerFactory.getLogger(AbstractDeploymentProcessor.class);

    public static final String JUMPING_TO_PARAM_NAME = "jumping_to";

    protected String env;
    protected String siteName;
    protected String targetId;
    protected String name;
    protected boolean initialized;

    // Config properties (populated on init)

    protected String label;
    protected String jumpTo;
    protected String[] includeFiles;
    protected String[] excludeFiles;
    protected boolean alwaysRun;

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


    @Override
    public boolean isPostDeployment() {
        return false;
    }

    @Override
    public void init(Configuration config) throws ConfigurationException, DeployerException {
        label = getStringProperty(config, DeploymentConstants.PROCESSOR_LABEL_CONFIG_KEY);
        jumpTo = getStringProperty(config, DeploymentConstants.PROCESSOR_JUMP_TO_CONFIG_KEY);
        includeFiles = getStringArrayProperty(config, DeploymentConstants.PROCESSOR_INCLUDE_FILES_CONFIG_KEY);
        excludeFiles = getStringArrayProperty(config, DeploymentConstants.PROCESSOR_EXCLUDE_FILES_CONFIG_KEY);
        alwaysRun = getBooleanProperty(config, DeploymentConstants.PROCESSOR_ALWAYS_RUN_CONFIG_KEY, false);

        doInit(config);
    }


    @Override
    public void destroy() throws DeployerException {
        doDestroy();
    }

    @Override
    public void execute(Deployment deployment) {
        ChangeSet originalChangeSet = deployment.getChangeSet();
        ChangeSet filteredChangeSet = getFilteredChangeSet(originalChangeSet);

        if (!isJumpToActive(deployment) && shouldExecute(deployment, filteredChangeSet)) {
            logger.info("----- < {} @ {} > -----", name, targetId);

            try {
                ChangeSet newChangeSet = doExecute(deployment, filteredChangeSet, originalChangeSet);
                if (newChangeSet != null) {
                    deployment.setChangeSet(newChangeSet);
                }

                if (StringUtils.isNotEmpty(jumpTo)) {
                    startJumpTo(deployment);
                }
            } catch (Exception e) {
                logger.error("Processor '" + name + "' for target '" + targetId + "' failed", e);
            } finally {
                logger.info("----- </ {} @ {} > -----", name, targetId);
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

            ChangeSet filteredChangeSet = new ChangeSet(matchedCreatedFiles, matchedUpdatedFiles, matchedDeletedFiles);
            filteredChangeSet.setUpdateDetails(changeSet.getUpdateDetails());
            filteredChangeSet.setUpdateLog(changeSet.getUpdateLog());

            return filteredChangeSet;
        } else {
            return changeSet;
        }
    }

    protected boolean shouldIncludeFile(String file) {
        return (ArrayUtils.isEmpty(includeFiles) || RegexUtils.matchesAny(file, includeFiles)) &&
               (ArrayUtils.isEmpty(excludeFiles) || !RegexUtils.matchesAny(file, excludeFiles));
    }

    protected boolean isJumpToActive(Deployment deployment) {
        String jumpingTo = (String) deployment.getParam(JUMPING_TO_PARAM_NAME);
        if (StringUtils.isEmpty(jumpingTo)) {
            return false;
        } else if (jumpingTo.equals(label)) {
            deployment.removeParam(JUMPING_TO_PARAM_NAME);

            return false;
        } else {
            return true;
        }
    }

    protected void startJumpTo(Deployment deployment) {
        logger.info("Jumping to processor of target '" + targetId + "' with label '" + jumpTo + "'");

        deployment.addParam(JUMPING_TO_PARAM_NAME, jumpTo);
    }

    protected abstract boolean shouldExecute(Deployment deployment, ChangeSet filteredChangeSet);

    protected abstract void doInit(Configuration config) throws ConfigurationException, DeployerException;

    protected abstract void doDestroy() throws DeployerException;

    protected abstract ChangeSet doExecute(Deployment deployment, ChangeSet filteredChangeSet,
                                           ChangeSet originalChangeSet) throws Exception;

}
