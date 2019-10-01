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
package org.craftercms.deployer.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.spring.ApacheCommonsConfiguration2PropertySource;
import org.craftercms.commons.validation.ValidationException;
import org.craftercms.commons.validation.ValidationResult;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.utils.handlebars.MissingValueHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

import static org.craftercms.deployer.impl.DeploymentConstants.*;
import static org.craftercms.deployer.utils.ConfigUtils.*;

/**
 * Default implementation of {@link TargetService}.
 *
 * @author avasquez
 */
@Component("targetService")
public class TargetServiceImpl implements TargetService, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(TargetServiceImpl.class);

    public static final String YAML_FILE_EXTENSION = "yaml";
    public static final String APPLICATION_CONTEXT_FILENAME_FORMAT = "%s-context.xml";
    public static final String CONFIG_PROPERTY_SOURCE_NAME = "targetConfig";
    public static final String CONFIG_BEAN_NAME = "targetConfig";

    public static final String TARGET_ENV_MODEL_KEY = "env";
    public static final String TARGET_SITE_NAME_MODEL_KEY = "site_name";
    public static final String TARGET_ID_MODEL_KEY = "target_id";
    public static final String TARGET_CRAFTER_SEARCH_MODEL_KEY = "use_crafter_search";

    protected File targetConfigFolder;
    protected Resource baseTargetYamlConfigResource;
    protected Resource baseTargetYamlConfigOverrideResource;
    protected Resource baseTargetContextResource;
    protected Resource baseTargetContextOverrideResource;
    protected String defaultTargetConfigTemplateName;
    protected Handlebars targetConfigTemplateEngine;
    protected ApplicationContext mainApplicationContext;
    protected DeploymentPipelineFactory deploymentPipelineFactory;
    protected TaskScheduler taskScheduler;
    protected ExecutorService taskExecutor;
    protected ProcessedCommitsStore processedCommitsStore;
    protected TargetLifecycleHooksResolver targetLifecycleHooksResolver;
    protected Set<Target> currentTargets;

    public TargetServiceImpl(
        @Value("${deployer.main.targets.config.folderPath}") File targetConfigFolder,
        @Value("${deployer.main.targets.config.baseYaml.location}") Resource baseTargetYamlConfigResource,
        @Value("${deployer.main.targets.config.baseYaml.overrideLocation}") Resource baseTargetYamlConfigOverrideResource,
        @Value("${deployer.main.targets.config.baseContext.location}") Resource baseTargetContextResource,
        @Value("${deployer.main.targets.config.baseContext.overrideLocation}") Resource baseTargetContextOverrideResource,
        @Value("${deployer.main.targets.config.templates.default}") String defaultTargetConfigTemplateName,
        @Autowired Handlebars targetConfigTemplateEngine,
        @Autowired ApplicationContext mainApplicationContext,
        @Autowired DeploymentPipelineFactory deploymentPipelineFactory,
        @Autowired TaskScheduler taskScheduler,
        @Autowired ExecutorService taskExecutor,
        @Autowired ProcessedCommitsStore processedCommitsStore,
        @Autowired TargetLifecycleHooksResolver targetLifecycleHooksResolver) {
        this.targetConfigFolder = targetConfigFolder;
        this.baseTargetYamlConfigResource = baseTargetYamlConfigResource;
        this.baseTargetYamlConfigOverrideResource = baseTargetYamlConfigOverrideResource;
        this.baseTargetContextResource = baseTargetContextResource;
        this.baseTargetContextOverrideResource = baseTargetContextOverrideResource;
        this.defaultTargetConfigTemplateName = defaultTargetConfigTemplateName;
        this.targetConfigTemplateEngine = targetConfigTemplateEngine;
        this.mainApplicationContext = mainApplicationContext;
        this.deploymentPipelineFactory = deploymentPipelineFactory;
        this.taskScheduler = taskScheduler;
        this.taskExecutor = taskExecutor;
        this.processedCommitsStore = processedCommitsStore;
        this.targetLifecycleHooksResolver = targetLifecycleHooksResolver;
        this.currentTargets = new CopyOnWriteArraySet<>();
    }

    @PostConstruct
    public void init() throws DeployerException {
        if (!targetConfigFolder.exists()) {
            logger.info("Target config folder " + targetConfigFolder + " doesn't exist. Creating it");

            try {
                FileUtils.forceMkdir(targetConfigFolder);
            } catch (IOException e) {
                throw new DeployerException("Failed to create target config folder at " + targetConfigFolder);
            }
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Load all existing targets on startup
        try {
            List<Target> targets = resolveTargets();
            if (CollectionUtils.isEmpty(targets)) {
                logger.warn("No config files found under {}", targetConfigFolder.getAbsolutePath());
            }
        } catch (DeployerException e) {
            logger.error("Error while loading targets on startup", e);
        }
    }

    @PreDestroy
    public void destroy() {
        logger.info("Closing all targets...");

        if (CollectionUtils.isNotEmpty(currentTargets)) {
            currentTargets.forEach(Target::close);
        }
    }

    @Override
    public List<Target> getAllTargets() throws TargetServiceException {
        return new ArrayList<>(currentTargets);
    }

    @Override
    public boolean targetExists(String env, String siteName) throws TargetServiceException {
        String id = TargetImpl.getId(env, siteName);

        return findLoadedTargetById(id) != null;
    }

    @Override
    public Target getTarget(String env, String siteName) throws TargetNotFoundException {
        String id = TargetImpl.getId(env, siteName);
        Target target = findLoadedTargetById(id);

        if (target != null) {
            return target;
        } else {
            throw new TargetNotFoundException(id, env, siteName);
        }
    }

    @Override
    public synchronized List<Target> resolveTargets() throws TargetServiceException {
        Collection<File> configFiles = getTargetConfigFiles();
        List<Target> targets = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(configFiles)) {
            closeTargetsWithNoConfigFile(configFiles);

            for (File file : configFiles) {
                Target target = resolveTargetFromConfigFile(file, false);
                targets.add(target);
            }
        }

        return targets;
    }

    @Override
    public synchronized Target createTarget(String env, String siteName, boolean replace, String templateName,
                                            boolean useCrafterSearch, Map<String, Object> templateParams)
        throws TargetAlreadyExistsException,
        TargetServiceException {
        String id = TargetImpl.getId(env, siteName);
        File configFile = new File(targetConfigFolder, id + "." + YAML_FILE_EXTENSION);

        if (!replace && configFile.exists()) {
            throw new TargetAlreadyExistsException(id, env, siteName);
        } else {
            createConfigFromTemplate(env, siteName, id, templateName, useCrafterSearch, templateParams, configFile);
        }

        return resolveTargetFromConfigFile(configFile, true);
    }

    @Override
    public synchronized void deleteTarget(String env, String siteName) throws TargetNotFoundException,
                                                                              TargetServiceException {
        Target target = getTarget(env, siteName);
        String id = target.getId();

        logger.info("Removing loaded target '{}'", id);

        currentTargets.remove(target);

        target.delete();

        try {
            processedCommitsStore.delete(id);
        } catch (DeployerException e) {
            throw new TargetServiceException("Error while deleting processed commit from store for target '" + id +
                                             "'", e);
        }

        File configFile =  target.getConfigurationFile();
        if (configFile.exists()) {
            logger.info("Deleting target configuration file at {}", configFile);

            FileUtils.deleteQuietly(configFile);
        }

        File contextFile = new File(targetConfigFolder, String.format(APPLICATION_CONTEXT_FILENAME_FORMAT,
                                                                      configFile.getName()));
        if (contextFile.exists()) {
            logger.info("Deleting target context file at {}", contextFile);

            FileUtils.deleteQuietly(contextFile);
        }
    }

    protected Collection<File> getTargetConfigFiles() throws TargetServiceException {
        if (targetConfigFolder.exists()) {
            return FileUtils.listFiles(targetConfigFolder, new CustomConfigFileFilter(), null);
        } else {
            logger.warn("Config folder {} doesn't exist. Trying to create it...", targetConfigFolder.getAbsolutePath());

            try {
                FileUtils.forceMkdir(targetConfigFolder);
            } catch (IOException e) {
                throw new TargetServiceException("Unable to create config folder " + targetConfigFolder, e);
            }

            return Collections.emptyList();
        }
    }

    protected void closeTargetsWithNoConfigFile(Collection<File> configFiles) {
        if (CollectionUtils.isNotEmpty(currentTargets)) {
            currentTargets.removeIf(target -> {
                File configFile = target.getConfigurationFile();
                if (!configFiles.contains(configFile)) {
                    logger.info("Config file {} doesn't exist anymore for target '{}'. Closing target...",
                                configFile, target.getId());

                    target.close();

                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    protected Target resolveTargetFromConfigFile(File configFile, boolean create) throws TargetServiceException {
        String baseName = FilenameUtils.getBaseName(configFile.getName());
        File contextFile = new File(targetConfigFolder, String.format(APPLICATION_CONTEXT_FILENAME_FORMAT, baseName));
        Target target = findLoadedTargetByConfigFile(configFile);

        if (target != null) {
            // Check if the YAML config file or the app context file have changed since target load.
            long yamlLastModified = configFile.exists() ? configFile.lastModified() : 0;
            long contextLastModified = contextFile.exists()? contextFile.lastModified() : 0;
            long targetLoadedDate = target.getLoadDate().toInstant().toEpochMilli();

            // Refresh if the files have been modified.
            if (yamlLastModified >= targetLoadedDate || contextLastModified >= targetLoadedDate) {
                logger.info("Configuration files haven been updated for '{}'. The target will be reloaded.",
                            target.getId());

                target.close();

                currentTargets.remove(target);

                target = null;
            }
        } else {
            logger.info("No loaded target found for configuration file {}", configFile);
        }

        if (target == null) {
            logger.info("Loading target for configuration file {}", configFile);

            target = loadTarget(configFile, contextFile, create);
            currentTargets.add(target);
        }

        return target;
    }

    @SuppressWarnings("unchecked")
    protected Target loadTarget(File configFile, File contextFile, boolean create) throws TargetServiceException {
        try {
            HierarchicalConfiguration config = loadConfiguration(configFile);
            String env = getRequiredStringProperty(config, TARGET_ENV_CONFIG_KEY);
            String siteName = getRequiredStringProperty(config, TARGET_SITE_NAME_CONFIG_KEY);
            String targetId = TargetImpl.getId(env, siteName);
            String localRepoPath = getRequiredStringProperty(config, TARGET_LOCAL_REPO_CONFIG_KEY);
            boolean crafterSearchEnabled = getBooleanProperty(config, TARGET_CRAFTER_SEARCH_CONFIG_KEY, false);

            config.setProperty(TARGET_ID_CONFIG_KEY, targetId);

            ConfigurableApplicationContext context = loadApplicationContext(config, contextFile);

            Target target = new TargetImpl(ZonedDateTime.now(), env, siteName, localRepoPath, configFile, config,
                                           context, taskExecutor, taskScheduler, targetLifecycleHooksResolver,
                                           deploymentPipelineFactory, crafterSearchEnabled);

            if (create) {
                executeCreateHooks(target);
            }

            startInit(target);

            return target;
        } catch (Exception e) {
            if (create) {
                FileUtils.deleteQuietly(configFile);
            }

            throw new TargetServiceException("Failed to load target for configuration file " + configFile, e);
        }
    }

    protected HierarchicalConfiguration loadConfiguration(File configFile) throws ConfigurationException {
        String configFilename = configFile.getPath();

        logger.debug("Loading target YAML config at {}", configFilename);

        HierarchicalConfiguration config = readYamlConfiguration(configFile);

        if (baseTargetYamlConfigResource.exists() || baseTargetYamlConfigOverrideResource.exists()) {
            CombinedConfiguration combinedConfig = new CombinedConfiguration(new OverrideCombiner());

            combinedConfig.addConfiguration(config);

            if (baseTargetYamlConfigOverrideResource.exists()) {
                logger.debug("Loading base target YAML config override at {}", baseTargetYamlConfigOverrideResource);

                combinedConfig.addConfiguration(readYamlConfiguration(baseTargetYamlConfigOverrideResource));
            }
            if (baseTargetYamlConfigResource.exists()) {
                logger.debug("Loading base target YAML config at {}", baseTargetYamlConfigResource);

                combinedConfig.addConfiguration(readYamlConfiguration(baseTargetYamlConfigResource));
            }

            return combinedConfig;
        } else {
            return config;
        }
    }

    protected ConfigurableApplicationContext loadApplicationContext(HierarchicalConfiguration config,
                                                                    File contextFile) throws ConfigurationException {
        GenericApplicationContext context = new GenericApplicationContext(mainApplicationContext);

        MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        propertySources.addFirst(new ApacheCommonsConfiguration2PropertySource(CONFIG_PROPERTY_SOURCE_NAME, config));

        context.getBeanFactory().registerSingleton(CONFIG_BEAN_NAME, config);

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);

        if (baseTargetContextResource.exists()) {
            logger.debug("Loading base target application context at {}", baseTargetContextResource);

            try {
                reader.loadBeanDefinitions(baseTargetContextResource);
            } catch (Exception e) {
                throw new ConfigurationException("Failed to load application context at " + baseTargetContextResource,
                                                 e);
            }
        }
        if (baseTargetContextOverrideResource.exists()) {
            logger.debug("Loading base target application context override at {}", baseTargetContextOverrideResource);

            try {
                reader.loadBeanDefinitions(baseTargetContextOverrideResource);
            } catch (Exception e) {
                throw new ConfigurationException("Failed to load application context at " +
                                                 baseTargetContextOverrideResource, e);
            }
        }
        if (contextFile.exists()) {
            logger.debug("Loading target application context at {}", contextFile);

            try (InputStream in = new BufferedInputStream(new FileInputStream(contextFile))) {
                reader.loadBeanDefinitions(new InputSource(in));
            } catch (Exception e) {
                throw new ConfigurationException("Failed to load application context at " + contextFile, e);
            }
        }

        context.refresh();

        return context;
    }

    protected void createConfigFromTemplate(String env, String siteName, String targetId, String templateName,
                                            boolean useCrafterSearch, Map<String, Object> templateParameters,
                                            File configFile) throws TargetServiceException {
        if (StringUtils.isEmpty(templateName)) {
            templateName = defaultTargetConfigTemplateName;
        }

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(TARGET_ENV_MODEL_KEY, env);
        templateModel.put(TARGET_SITE_NAME_MODEL_KEY, siteName);
        templateModel.put(TARGET_ID_MODEL_KEY, targetId);
        templateModel.put(TARGET_CRAFTER_SEARCH_MODEL_KEY, useCrafterSearch);

        if (MapUtils.isNotEmpty(templateParameters)) {
            templateModel.putAll(templateParameters);
        }

        logger.info("Creating new target YAML configuration at {} using template '{}'", configFile, templateName);

        try (Writer out = new BufferedWriter(new FileWriter(configFile))) {
            processConfigTemplate(templateName, templateModel, out);

            out.flush();
        } catch (IOException e) {
            throw new TargetServiceException("Unable to open writer to YAML configuration file " + configFile, e);
        } catch (TargetServiceException e) {
            FileUtils.deleteQuietly(configFile);

            throw e;
        }
    }

    protected void processConfigTemplate(String templateName, Object templateModel, Writer out)
            throws TargetServiceException {
        MissingValueHelper helper = MissingValueHelper.INSTANCE;

        try {
            Template template = targetConfigTemplateEngine.compile(templateName);
            template.apply(templateModel, out);
        } catch (IOException e) {
            throw new TargetServiceException("Processing of configuration template '" + templateName + "' failed", e);
        }

        ValidationResult result = helper.getValidationResult();

        helper.clearValidationResult();

        if (result != null && result.hasErrors()) {
            throw new TargetServiceException(new ValidationException(result));
        }
    }

    protected Target findLoadedTargetByConfigFile(File configFile) {
        if (CollectionUtils.isNotEmpty(currentTargets)) {
            return currentTargets.stream()
                                 .filter(target -> target.getConfigurationFile().equals(configFile))
                                 .findFirst()
                                 .orElse(null);
        } else {
            return null;
        }
    }

    protected Target findLoadedTargetById(String id) {
        if (CollectionUtils.isNotEmpty(currentTargets)) {
            return currentTargets.stream()
                                 .filter(target -> target.getId().equals(id))
                                 .findFirst()
                                 .orElse(null);
        } else {
            return null;
        }
    }

    protected void executeCreateHooks(Target target) throws Exception {
        List<TargetLifecycleHook> createHooks = targetLifecycleHooksResolver.getHooks(
                target.getConfiguration(), target.getApplicationContext(), CREATE_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY);

        logger.info("Executing create hooks for target '{}'", target.getId());

        for (TargetLifecycleHook hook : createHooks) {
            hook.execute(target);
        }
    }

    protected void startInit(Target target) {
        taskExecutor.execute(target::init);
    }

    protected class CustomConfigFileFilter extends AbstractFileFilter {

        @Override
        public boolean accept(File file) {
            String filename = file.getName();

            return !filename.equals(baseTargetYamlConfigResource.getFilename()) &&
                   !filename.equals(baseTargetYamlConfigOverrideResource.getFilename()) &&
                   filename.endsWith(YAML_FILE_EXTENSION);
        }
    }

}
