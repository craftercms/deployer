package org.craftercms.deployer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.CompositeTemplateLoader;
import com.github.jknack.handlebars.springmvc.SpringTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.core.cache.impl.CacheStoreAdapter;
import org.craftercms.core.cache.impl.store.NoopCacheStoreAdapter;
import org.craftercms.core.processors.ItemProcessor;
import org.craftercms.core.processors.impl.PageAwareIncludeDescriptorsProcessor;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.ProcessedCommitsStore;
import org.craftercms.deployer.impl.ProcessedCommitsStoreImpl;
import org.craftercms.deployer.impl.rest.ExceptionHandlers;
import org.craftercms.deployer.utils.handlebars.ListHelper;
import org.craftercms.deployer.utils.handlebars.MissingValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import static org.craftercms.deployer.DeployerApplication.CORE_APP_CONTEXT_LOCATION;

/**
 * Launcher class and Spring configuration entry point.
 *
 * @author avasquez
 */
@SpringBootApplication
@EnableScheduling
@ImportResource(CORE_APP_CONTEXT_LOCATION)
public class DeployerApplication extends WebMvcConfigurerAdapter implements SchedulingConfigurer  {

	private static final Logger logger = LoggerFactory.getLogger(DeployerApplication.class);

	public static final String CORE_APP_CONTEXT_LOCATION = "classpath:crafter/core/core-context.xml";

	public static final String DEFAULT_INCLUDE_ELEMENT_XPATH_QUERY = "//include";
	public static final String DEFAULT_DISABLED_INCLUDE_NODE_XPATH_QUERY = "../disableFlattening";
	public static final String DEFAULT_PAGES_PATH_PATTERN = "^/?site/website/.*$";

	@Value("${deployer.main.targets.scan.scheduling.enabled}")
	private boolean scheduledTargetScanEnabled;
	@Value("${deployer.main.targets.scan.scheduling.cron}")
	private String scheduledTargetScanCron;
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
	@Autowired
	private TargetService targetService;
	@Autowired
	private ContentStoreService contentStoreService;

	public static void main(String[] args) {
		SpringApplication.run(DeployerApplication.class, args);
	}

	@Bean("crafter.cacheStoreAdapter")
	public CacheStoreAdapter cacheStoreAdapter() {
		return new NoopCacheStoreAdapter();
	}

	@Bean
	public ItemProcessor includeDescriptorsProcessor() {
		PageAwareIncludeDescriptorsProcessor processor = new PageAwareIncludeDescriptorsProcessor();
		processor.setIncludeElementXPathQuery(DEFAULT_INCLUDE_ELEMENT_XPATH_QUERY);
		processor.setDisabledIncludeNodeXPathQuery(DEFAULT_DISABLED_INCLUDE_NODE_XPATH_QUERY);
		processor.setPagesPathPattern(DEFAULT_PAGES_PATH_PATTERN);
		processor.setIncludedItemsProcessor(processor);
		processor.setContentStoreService(contentStoreService);

		return processor;
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

	@Bean(destroyMethod="shutdown")
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(taskSchedulerPoolSize);

		return taskScheduler;
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

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.defaultContentType(MediaType.APPLICATION_JSON_UTF8);
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(taskScheduler());
		configureTargetScanTask(taskRegistrar);
	}

	private void configureTargetScanTask(ScheduledTaskRegistrar taskRegistrar) {
		if (scheduledTargetScanEnabled && StringUtils.isNotEmpty(scheduledTargetScanCron)) {
			logger.info("Target scan scheduled with cron {}", scheduledTargetScanCron);

			Runnable task = () -> {

				try {
					targetService.resolveTargets();
				} catch (DeployerException e) {
					logger.error("Scheduled target scan failed", e);
				}

			};

			taskRegistrar.addCronTask(task, scheduledTargetScanCron);
		}
	}

}
