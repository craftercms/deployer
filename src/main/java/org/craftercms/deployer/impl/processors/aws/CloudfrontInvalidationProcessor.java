package org.craftercms.deployer.impl.processors.aws;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractMainDeploymentProcessor;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.CreateInvalidationResult;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;

public class CloudfrontInvalidationProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CloudfrontInvalidationProcessor.class);

    protected String[] distributions;
    protected String prefix;

    @Override
    protected void doInit(final Configuration config) throws DeployerException {
        distributions = ConfigUtils.getStringArrayProperty(config, "distributions");
        prefix = ConfigUtils.getStringProperty(config, "prefix", null);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ChangeSet doExecute(final Deployment deployment, final ProcessorExecution execution,
                                  final ChangeSet filteredChangeSet) throws DeployerException {

        logger.info("Performing Cloudfront invalidation...");

        AmazonCloudFront client = AmazonCloudFrontClientBuilder.defaultClient();

        List<String> changedFiles =
            ((List<String>) ListUtils.union(filteredChangeSet.getCreatedFiles(), filteredChangeSet.getUpdatedFiles()))
            .stream()
            .map(file -> StringUtils.removeFirst(file, prefix))
            .collect(Collectors.toList());
        Paths paths = new Paths().withItems(changedFiles).withQuantity(changedFiles.size());

        logger.info("Will invalidate {} files", changedFiles.size());

        for(String distribution : distributions) {
            String caller = UUID.randomUUID().toString();
            logger.info("Creating invalidation for distribution {} with reference {}", distribution, caller);
            InvalidationBatch batch = new InvalidationBatch().withPaths(paths).withCallerReference(caller);
            CreateInvalidationRequest request = new CreateInvalidationRequest(distribution, batch);
            CreateInvalidationResult result = client.createInvalidation(request);
            logger.info("Created invalidation {} for distribution {}", result.getInvalidation().getId(), distribution);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    @Override
    public void destroy() {
        // nothing to do...
    }

}
