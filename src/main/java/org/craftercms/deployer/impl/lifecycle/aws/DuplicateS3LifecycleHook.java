/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.impl.lifecycle.aws;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.aws.AwsUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.git.utils.GitUtils;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.ProcessedCommitsStore;
import org.craftercms.deployer.impl.lifecycle.AbstractLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsClientBuilderConfigurer;
import org.craftercms.deployer.utils.aws.AwsS3ClientBuilderConfigurer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;
import static org.craftercms.commons.config.ConfigUtils.getBooleanProperty;
import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.utils.aws.AwsS3Utils.getBucket;
import static org.craftercms.deployer.utils.aws.AwsS3Utils.getS3BaseKey;

/**
 * Lifecycle hook that duplicates content from source to new target S3 bucket.
 * It will list all files in the source bucket (filtering out .blob files if ignoreBlobs is true) and copy
 * them to the target bucket.
 */
public class DuplicateS3LifecycleHook extends AbstractLifecycleHook {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public static final String CONFIG_KEY_IGNORE_BLOBS = "ignoreBlobs";

    protected static final String CONFIG_KEY_SOURCE_CONFIG = "sourceConfig";
    protected static final String CONFIG_KEY_LOCAL_REPO_URL = "localRepoPath";
    protected static final String CONFIG_KEY_URL = "url";
    protected static final String DELIMITER = "/";
    private final String siteName;
    private final String sourceSiteName;
    private final ProcessedCommitsStore processedCommitsStore;
    private final TargetService targetService;
    private final String blobExtension;
    protected final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private boolean ignoreBlobs;
    private AwsClientBuilderConfigurer<AmazonS3ClientBuilder> builderConfigurer;
    private AmazonS3URI s3Url;
    private AmazonS3URI srcS3Url;
    private String srcLocalRepoPath;

    @ConstructorProperties({"siteName", "sourceSiteName", "processedCommitsStore",
            "targetService", "blobExtension", "threadPoolTaskExecutor"})
    public DuplicateS3LifecycleHook(String siteName, String sourceSiteName, ProcessedCommitsStore processedCommitsStore,
                                    TargetService targetService, String blobExtension, ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.siteName = siteName;
        this.sourceSiteName = sourceSiteName;
        this.processedCommitsStore = processedCommitsStore;
        this.targetService = targetService;
        this.blobExtension = blobExtension;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException, DeployerException {
        builderConfigurer = new AwsS3ClientBuilderConfigurer(config);
        s3Url = new AmazonS3URI(appendIfMissing(getRequiredStringProperty(config, CONFIG_KEY_URL), DELIMITER));

        ignoreBlobs = getBooleanProperty(config, CONFIG_KEY_IGNORE_BLOBS, true);

        Configuration srcTargetConfig = config.subset(CONFIG_KEY_SOURCE_CONFIG);
        srcS3Url = new AmazonS3URI(appendIfMissing(getRequiredStringProperty(srcTargetConfig, CONFIG_KEY_URL), DELIMITER));
        srcLocalRepoPath = getRequiredStringProperty(srcTargetConfig, CONFIG_KEY_LOCAL_REPO_URL);

    }

    protected AmazonS3 buildClient(AwsClientBuilderConfigurer<AmazonS3ClientBuilder> builderConfigurer) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builderConfigurer.configureClientBuilder(builder);
        return builder.build();
    }

    /**
     * Get the list of paths to copy between buckets.
     * This method will list all files in the source bucket (filtering out .blob files if ignoreBlobs is true).
     *
     * @return List of paths to copy between buckets
     * @throws DeployerException If an error occurs while retrieving the list of paths
     */
    private List<String> getItemPathList(Target target) throws DeployerException {
        Target srcTarget = targetService.getTarget(target.getEnv(), sourceSiteName);

        List<String> paths = new LinkedList<>();
        try (Git git = GitUtils.openRepository(new File(srcLocalRepoPath))) {
            Repository repo = git.getRepository();
            try (TreeWalk walk = new TreeWalk(repo)) {
                walk.reset(); // Drop tree, we are going to add a new one
                walk.setRecursive(true);
                walk.addTree(GitUtils.getTreeForCommit(repo, processedCommitsStore.load(srcTarget.getId())));
                if (ignoreBlobs) {
                    walk.setFilter(PathSuffixFilter.create(blobExtension).negate());
                }
                while (walk.next()) {
                    paths.add(walk.getPathString());
                }
            }
        } catch (IOException e) {
            throw new DeployerException(format("Error while retrieving list of paths to copy between buckets during duplication from site '%s' to '%s'", sourceSiteName, siteName), e);
        }

        return paths;
    }

    @Override
    protected void doExecute(Target target) throws DeployerException {
        AmazonS3 client = buildClient(builderConfigurer);
        logger.info("Starting S3 content duplicate from '{}' for site '{}' to '{}' for site '{}'", srcS3Url, sourceSiteName, s3Url, siteName);

        List<String> paths = getItemPathList(target);
        try {
            AwsUtils.copyObjects(client, threadPoolTaskExecutor::getThreadPoolExecutor, getBucket(srcS3Url, sourceSiteName), getS3BaseKey(srcS3Url, sourceSiteName),
                    getBucket(s3Url, siteName), getS3BaseKey(s3Url, siteName), paths);
        } catch (InterruptedException e) {
            throw new DeployerException(format("Interrupted while waiting for S3 content duplication from site '%s' to '%s'", sourceSiteName, siteName), e);
        }
        logger.info("Completed S3 content duplicate from '{}' for site '{}' to '{}' for site '{}'", srcS3Url, sourceSiteName, s3Url, siteName);
    }

}
