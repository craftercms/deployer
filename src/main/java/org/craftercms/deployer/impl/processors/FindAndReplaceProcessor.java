package org.craftercms.deployer.impl.processors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} to replace a pattern on the
 * content of the created or updated files of a {@link Deployment}.
 * @author joseross
 */
public class FindAndReplaceProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FindAndReplaceProcessor.class);

    public static final String CONFIG_KEY_TEXT_PATTERN = "textPattern";
    public static final String CONFIG_KEY_REPLACEMENT = "replacement";

    /**
     * URL for the local git repository.
     */
    protected String localRepoUrl;

    /**
     * Regular expression to search in files.
     */
    protected String textPattern;

    /**
     * Expression to replace the matches.
     */
    protected String replacement;

    @Required
    public void setLocalRepoUrl(final String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit(final Configuration config) throws DeployerException {
        textPattern = ConfigUtils.getRequiredStringProperty(config, CONFIG_KEY_TEXT_PATTERN);
        replacement = ConfigUtils.getRequiredStringProperty(config, CONFIG_KEY_REPLACEMENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ChangeSet doExecute(final Deployment deployment, final ProcessorExecution execution,
                                  final ChangeSet filteredChangeSet) throws DeployerException {

        logger.info("Performing find and replace...");
        logger.debug("Pattern '{}' will be replaced with '{}'", textPattern, replacement);

        for(String file :
            ListUtils.union(filteredChangeSet.getCreatedFiles(), filteredChangeSet.getUpdatedFiles())) {

            try {
                logger.info("Performing find and replace for file {}", file);
                Path path = Paths.get(localRepoUrl, file);
                String content = new String(Files.readAllBytes(path));
                String updated = content.replaceAll(textPattern, replacement);
                if(StringUtils.equals(content, updated)) {
                    logger.debug("No matches found on file {}", file);
                } else {
                    logger.debug("Updating content for file {}", file);
                    Files.write(path, updated.getBytes());
                }
            } catch (Exception e) {
                logger.error("Error performing find and replace on file " + file, e);
                throw new DeployerException("Error performing find and replace on file " + file, e);
            }
        }

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // nothing to do...
    }

}
