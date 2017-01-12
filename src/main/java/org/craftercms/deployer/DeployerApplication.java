package org.craftercms.deployer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.io.CompositeTemplateLoader;
import com.github.jknack.handlebars.springmvc.SpringTemplateLoader;

import java.io.IOException;
import java.lang.reflect.Array;

import freemarker.template.TemplateException;
import org.craftercms.deployer.api.TargetManager;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@SpringBootApplication
@EnableScheduling
public class DeployerApplication implements SchedulingConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(DeployerApplication.class);

	@Value("${deployer.main.target.scan.scheduling.enabled}")
	private boolean scheduledTargetScanEnabled;
	@Value("${deployer.main.target.scan.scheduling.cron}")
	private String scheduledTargetScanCron;
	@Value("${deployer.main.scheduling.poolSize}")
	private int schedulerPoolSize;
	@Value("${deployer.main.target.config.templates.location}")
	private String targetConfigTemplatesLocation;
	@Value("${deployer.main.target.config.templates.overrideLocation}")
	private String targetConfigTemplatesOverrideLocation;
	@Value("${deployer.main.target.config.templates.suffix}")
	private String targetConfigTemplatesSuffix;
	@Value("${deployer.main.target.config.templates.encoding}")
	private String targetConfigTemplatesEncoding;
	@Autowired
	private TargetManager targetManager;

	public static void main(String[] args) {
		SpringApplication.run(DeployerApplication.class, args);
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
		taskScheduler.setPoolSize(schedulerPoolSize);

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

		handlebars.registerHelper("list", (context, options) -> {
			if (context instanceof Iterable) {
				StringBuilder ret = new StringBuilder();
				Iterable iterable = (Iterable)context;

				for (Object elem : iterable) {
					ret.append(options.fn(elem));
				}

				return ret.toString();
			} else if (context.getClass().isArray()) {
				StringBuilder ret = new StringBuilder();
				int length = Array.getLength(context);

				for (int i = 0; i < length; i++) {
					ret.append(options.fn(Array.get(context, i)));
				}

				return ret.toString();
			} else {
				return options.fn(context);
			}
		});

		return handlebars;
	}

	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
		taskRegistrar.setScheduler(taskScheduler());
		configureTargetScanTask(taskRegistrar);
	}

	private void configureTargetScanTask(ScheduledTaskRegistrar taskRegistrar) {
		if (scheduledTargetScanEnabled) {
			logger.info("Target scan scheduled with cron {}", scheduledTargetScanCron);

			Runnable task = () -> {

				try {
					targetManager.getAllTargets();
				} catch (DeploymentException e) {
					logger.error("Scheduled target scan failed", e);
				}

			};

			taskRegistrar.addCronTask(task, scheduledTargetScanCron);
		}
	}

}
