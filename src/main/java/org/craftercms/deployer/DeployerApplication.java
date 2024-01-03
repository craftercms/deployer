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

package org.craftercms.deployer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;

import freemarker.template.TemplateException;
import org.craftercms.commons.config.ConfigurationResolver;
import org.craftercms.commons.config.ConfigurationResolverImpl;
import org.craftercms.commons.config.EncryptionAwareConfigurationReader;
import org.craftercms.commons.config.PublishingTargetResolver;
import org.craftercms.commons.crypto.CryptoException;
import org.craftercms.commons.crypto.TextEncryptor;
import org.craftercms.commons.crypto.impl.PbkAesTextEncryptor;
import org.craftercms.commons.git.utils.AuthConfiguratorFactory;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.events.DeploymentEventsStore;
import org.craftercms.deployer.impl.ProcessedCommitsStore;
import org.craftercms.deployer.impl.ProcessedCommitsStoreImpl;
import org.craftercms.deployer.impl.events.FileBasedDeploymentEventsStore;
import org.craftercms.deployer.utils.core.TargetAwarePublishingTargetResolver;
import org.craftercms.deployer.utils.handlebars.ListHelper;
import org.craftercms.deployer.utils.handlebars.MissingValueHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.CompositeTemplateLoader;
import com.github.jknack.handlebars.springmvc.SpringTemplateLoader;

import static org.craftercms.deployer.DeployerApplication.CORE_APP_CONTEXT_LOCATION;

/**
 * Launcher class and Spring configuration entry point.
 *
 * @author avasquez
 */
@SpringBootApplication
@EnableScheduling
@ImportResource(CORE_APP_CONTEXT_LOCATION)
public class DeployerApplication implements WebMvcConfigurer  {

	public static final String CORE_APP_CONTEXT_LOCATION = "classpath:crafter/core/core-context.xml";

	@Value("${deployer.main.taskScheduler.poolSize}")
	private int taskSchedulerPoolSize;
	@Value("${deployer.main.targets.config.templates.location}")
	private String targetConfigTemplatesLocation;
	@Value("${deployer.main.targets.config.templates.overrideLocation}")
	private String targetConfigTemplatesOverrideLocation;
	@Value("${deployer.main.targets.config.templates.suffix}")
	private String targetConfigTemplatesSuffix;
	@Value("${deployer.main.targets.config.templates.encoding}")
	private String targetConfigTemplatesEncoding;
	@Value("${deployer.main.deployments.processedCommits.folderPath}")
	private File processedCommitsFolder;

	@Value("${deployer.main.deployments.pool.size}")
	private int deploymentPoolSize;
	@Value("${deployer.main.deployments.pool.max}")
	private int deploymentPoolMaxSize;
	@Value("${deployer.main.deployments.pool.queue}")
	private int deploymentPoolQueue;
	@Value("${deployer.main.deployments.pool.name}")
	private String deploymentPoolName;
	@Value("${deployer.main.deployments.pool.prefix}")
	private String deploymentPoolPrefix;

	@Autowired
	private TargetService targetService;

	public static void main(String[] args) {
		SpringApplication.run(DeployerApplication.class, args);
	}

	@Bean
	public ProcessedCommitsStore processedCommitsStore() {
		ProcessedCommitsStoreImpl store = new ProcessedCommitsStoreImpl();
		store.setStoreFolder(processedCommitsFolder);

		return store;
	}

	@Bean
	@Primary
	public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
		ObjectMapper objectMapper = builder.createXmlMapper(false).build();
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return objectMapper;
	}

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder builder) {
		return builder.build();
	}

	@Bean(destroyMethod="shutdown")
	public ThreadPoolTaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(taskSchedulerPoolSize);

		return taskScheduler;
	}

	@Bean(destroyMethod = "shutdownNow")
	public ExecutorService deploymentTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(deploymentPoolSize);
		executor.setMaxPoolSize(deploymentPoolMaxSize);
		executor.setQueueCapacity(deploymentPoolQueue);
		executor.setThreadGroupName(deploymentPoolName);
		executor.setThreadNamePrefix(deploymentPoolPrefix);
		executor.initialize();

		return executor.getThreadPoolExecutor();
	}

	@Bean
	public Handlebars targetConfigTemplateEngine(ResourceLoader resourceLoader) throws IOException, TemplateException {
		SpringTemplateLoader templateOverridesLoader = new SpringTemplateLoader(resourceLoader);
		templateOverridesLoader.setPrefix(targetConfigTemplatesOverrideLocation);
		templateOverridesLoader.setSuffix(targetConfigTemplatesSuffix);

		SpringTemplateLoader templateLoader = new SpringTemplateLoader(resourceLoader);
		templateLoader.setPrefix(targetConfigTemplatesLocation);
		templateLoader.setSuffix(targetConfigTemplatesSuffix);

		CompositeTemplateLoader compositeTemplateLoader = new CompositeTemplateLoader(templateOverridesLoader, templateLoader);

		Handlebars handlebars = new Handlebars(compositeTemplateLoader);
		handlebars.prettyPrint(true);

		handlebars.registerHelper(ListHelper.NAME, ListHelper.INSTANCE);
		handlebars.registerHelperMissing(MissingValueHelper.INSTANCE);

		return handlebars;
	}

	@Bean("crafter.textEncryptor")
	public TextEncryptor textEncryptor(@Value("${deployer.main.security.encryption.key}") String key,
                                       @Value("${deployer.main.security.encryption.salt}") String salt)
		throws CryptoException {
		return new PbkAesTextEncryptor(key, salt);
	}

	@Bean("crafter.configurationReader")
	public EncryptionAwareConfigurationReader configurationReader(@Autowired TextEncryptor textEncryptor,
																  @Value("${deployer.main.config.yamlMaxAliasesForCollections}") int yamlMaxAliasesForCollections) {
		return new EncryptionAwareConfigurationReader(textEncryptor, yamlMaxAliasesForCollections);
	}

	@Bean("crafter.publishingTargetResolver")
    public PublishingTargetResolver publishingTargetResolver(
			@Value("${deployer.main.targets.config.blob.staging.pattern}") String stagingNamePattern) {
	    return new TargetAwarePublishingTargetResolver(stagingNamePattern);
    }

    @Bean("crafter.configurationResolver")
    public ConfigurationResolver configurationResolver(
            @Value("${deployer.main.config.environment.active}") String environment,
            @Value("${deployer.main.config.environment.basePath}") String basePath,
            @Value("${deployer.main.config.environment.envPath}") String envPath,
            @Autowired EncryptionAwareConfigurationReader configurationReader) {
	    return new ConfigurationResolverImpl(environment, basePath, envPath, configurationReader);
    }

    @Bean
    public DeploymentEventsStore<Properties, Path> deploymentEventsStore(
    		@Value("${deployer.main.deployments.events.folderPath}") String folderPath,
			@Value("${deployer.main.deployments.events.filePattern}") String filePattern) {
		return new FileBasedDeploymentEventsStore(folderPath, filePattern);
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON);
	}

	@Bean
	public AuthConfiguratorFactory gitAuthenticationConfiguratorFactory(
			@Value("${deployer.main.security.ssh.config}") File sshConfig) {
		return new AuthConfiguratorFactory(sshConfig);
	}

}
