/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.deployer.impl.processors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} to replace a pattern on the
 * content of the created or updated files of a {@link Deployment}.
 * <p><strong>Note:</strong> the files changed by this processor will not be committed to the git repository and
 * will be discarded when the next deployment starts.</p>
 *
 * Can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>textPattern:</strong> Regular expression to search in files</li>
 *     <li><strong>replacement:</strong> Expression to replace the matches</li>
 * </ul>
 *
 * @author joseross
 */
public class FindAndReplaceProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FindAndReplaceProcessor.class);

    protected static final String CONFIG_KEY_TEXT_PATTERN = "textPattern";
    protected static final String CONFIG_KEY_REPLACEMENT = "replacement";

    /**
     * URL for the local git repository.
     */
    protected String localRepoUrl;

    // Config properties (populated on init)

    /**
     * Regular expression to search in files.
     */
    protected String textPattern;
    /**
     * Expression to replace the matches.
     */
    protected String replacement;

    public void setLocalRepoUrl(final String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit(final Configuration config) throws ConfigurationException {
        textPattern = getRequiredStringProperty(config, CONFIG_KEY_TEXT_PATTERN);
        replacement = getRequiredStringProperty(config, CONFIG_KEY_REPLACEMENT);

        // use true as default for backward compatibility
        failDeploymentOnFailure = config.getBoolean(FAIL_DEPLOYMENT_CONFIG_KEY, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        logger.info("Performing find & replace. Pattern '{}' will be replaced with '{}'...", textPattern, replacement);

        for(String file :
            ListUtils.union(filteredChangeSet.getCreatedFiles(), filteredChangeSet.getUpdatedFiles())) {

            try {
                Path path = Paths.get(localRepoUrl, file);
                String content = new String(Files.readAllBytes(path));
                String updated = content.replaceAll(textPattern, replacement);

                if(StringUtils.equals(content, updated)) {
                    logger.debug("No matches found for file {}", file);
                } else {
                    logger.debug("Writing changes to file {}", file);
                    Files.write(path, updated.getBytes());
                }
            } catch (Exception e) {
                throw new DeployerException("Error performing find and replace on file " + file, e);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

}
