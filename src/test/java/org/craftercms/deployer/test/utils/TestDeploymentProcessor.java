package org.craftercms.deployer.test.utils;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility processor that just logs that it's running.
 *
 * @author avasquez
 */
public class TestDeploymentProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TestDeploymentProcessor.class);

    @Override
    public void destroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected void doInit(Configuration config) throws DeployerException {
        // Do nothing
    }

    @Override
    protected ChangeSet doExecute(Deployment deployment, ProcessorExecution execution,
                                  ChangeSet filteredChangeSet) throws DeployerException {
        logger.info("Test deployment processor running");

        return filteredChangeSet;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return true;
    }

}
