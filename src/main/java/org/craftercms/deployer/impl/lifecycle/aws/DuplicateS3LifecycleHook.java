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

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.aws.AwsUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.http.HttpUtils;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.ProcessedCommitsStore;
import org.craftercms.deployer.impl.lifecycle.AbstractLifecycleHook;
import org.craftercms.deployer.utils.aws.AwsS3ClientBuilderConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import software.amazon.awssdk.services.s3.*;

import java.beans.ConstructorProperties;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static final String GIT_ROOT = ".git" + File.separator;

    protected static final String CONFIG_KEY_IGNORE_BLOBS = "ignoreBlobs";

    protected static final String CONFIG_KEY_SOURCE_CONFIG = "sourceConfig";
    protected static final String CONFIG_KEY_LOCAL_REPO_URL = "localRepoPath";
    protected static final String CONFIG_KEY_URL = "url";
    protected static final String DELIMITER = "/";
    private final String siteName;
    private final String sourceSiteName;
    private final ProcessedCommitsStore processedCommitsStore;
    private final TargetService targetService;
    private final String blobExtension;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    private boolean ignoreBlobs;
    private AwsS3ClientBuilderConfigurer builderConfigurer;
    private S3Uri s3Url;
    private S3Uri srcS3Url;
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
        S3Utilities s3Utilities = buildClient(builderConfigurer).utilities();
        String uri = HttpUtils.encodeUrlMacro(appendIfMissing(getRequiredStringProperty(config, CONFIG_KEY_URL), DELIMITER));
        s3Url = s3Utilities.parseUri(URI.create(uri));
        ignoreBlobs = getBooleanProperty(config, CONFIG_KEY_IGNORE_BLOBS, true);

        Configuration srcTargetConfig = config.subset(CONFIG_KEY_SOURCE_CONFIG);
        String srcUri = HttpUtils.encodeUrlMacro(appendIfMissing(getRequiredStringProperty(srcTargetConfig, CONFIG_KEY_URL), DELIMITER));
        srcS3Url = s3Utilities.parseUri(URI.create(srcUri));
        srcLocalRepoPath = getRequiredStringProperty(srcTargetConfig, CONFIG_KEY_LOCAL_REPO_URL);
    }

    protected S3AsyncClient buildClient(AwsS3ClientBuilderConfigurer builderConfigurer) {
        S3AsyncClientBuilder builder = S3AsyncClient.builder();
        builderConfigurer.configureClientBuilder(builder);
        return builder.build();
    }

    /**
     * Get the list of paths to copy between buckets.
     * This method will list all files in the source repository (filtering out .blob files if ignoreBlobs is true).
     *
     * @return List of paths to copy between buckets
     * @throws DeployerException If an error occurs while retrieving the list of paths
     */
    private List<String> getItemPathList(Path repoPath) throws DeployerException {
        try (Stream<Path> paths = Files.walk(repoPath)) {
            return paths.filter(Files::isRegularFile)
                    .map(repoPath::relativize)
                    .map(Path::toString)
                    .filter(p -> !p.startsWith(GIT_ROOT))
                    .filter(p -> !ignoreBlobs || !p.endsWith(blobExtension))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new DeployerException(format("Error while retrieving list of paths to copy between buckets during duplication from site '%s' to '%s'", sourceSiteName, siteName), e);
        }
    }

    @Override
    protected void doExecute(Target target) throws DeployerException {
        logger.info("Starting S3 content duplicate from '{}' for site '{}' to '{}' for site '{}'", srcS3Url, sourceSiteName, s3Url, siteName);
        Path repoPath = Path.of(this.srcLocalRepoPath);
        if (!repoPath.toFile().exists()) {
            // Unpublished site, no local repo
            logger.info("Local repository path '{}' does not exist, skipping S3 content duplication", this.srcLocalRepoPath);
            return;
        }
        List<String> paths = getItemPathList(repoPath);
        S3AsyncClient client = buildClient(builderConfigurer);
        try {
            AwsUtils.copyObjects(client, threadPoolTaskExecutor.getThreadPoolExecutor(), getBucket(srcS3Url, sourceSiteName), getS3BaseKey(srcS3Url, sourceSiteName),
                    getBucket(s3Url, siteName), getS3BaseKey(s3Url, siteName), paths);
        } catch (Exception e) {
            throw new DeployerException(format("Exception while waiting for S3 content duplication from site '%s' to '%s'", sourceSiteName, siteName), e);
        }
        logger.info("Completed S3 content duplicate from '{}' for site '{}' to '{}' for site '{}'", srcS3Url, sourceSiteName, s3Url, siteName);
    }

}
