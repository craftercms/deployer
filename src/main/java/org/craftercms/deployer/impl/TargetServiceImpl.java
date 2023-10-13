/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.config.EncryptionAwareConfigurationReader;
import org.craftercms.commons.spring.ApacheCommonsConfiguration2PropertySource;
import org.craftercms.commons.upgrade.UpgradeManager;
import org.craftercms.commons.validation.ValidationException;
import org.craftercms.commons.validation.ValidationResult;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.deployer.utils.config.yaml.KeyOrderedYAMLConfiguration;
import org.craftercms.deployer.utils.handlebars.MissingValueHelper;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.eclipse.jgit.lib.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

import static java.lang.String.format;
import static org.apache.commons.collections.CollectionUtils.isEmpty;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.craftercms.deployer.impl.DeploymentConstants.*;
import static org.craftercms.commons.config.ConfigUtils.*;

/**
 * Default implementation of {@link TargetService}.
 *
 * @author avasquez
 */
@SuppressWarnings("rawtypes")
@Component("targetService")
@DependsOn("crafter.cacheStoreAdapter")
public class TargetServiceImpl implements TargetService, ApplicationListener<ApplicationReadyEvent>,
        InitializingBean, DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(TargetServiceImpl.class);

    public static final String YAML_FILE_EXTENSION = "yaml";
    public static final String APPLICATION_CONTEXT_FILENAME_FORMAT = "%s-context.xml";
    public static final String CONFIG_PROPERTY_SOURCE_NAME = "targetConfig";
    public static final String CONFIG_BEAN_NAME = "targetConfig";

    public static final String TARGET_ENV_MODEL_KEY = "env";
    public static final String TARGET_SITE_NAME_MODEL_KEY = "site_name";
    public static final String TARGET_ID_MODEL_KEY = "target_id";

    protected final File targetConfigFolder;
    protected final Resource baseTargetYamlConfigResource;
    protected final Resource baseTargetYamlConfigOverrideResource;
    protected final Resource baseTargetContextResource;
    protected final Resource baseTargetContextOverrideResource;
    protected final String defaultTargetConfigTemplateName;
    protected final Handlebars targetConfigTemplateEngine;
    protected final ApplicationContext mainApplicationContext;
    protected final DeploymentPipelineFactory deploymentPipelineFactory;
    protected final TaskScheduler taskScheduler;
    protected final ExecutorService taskExecutor;
    protected final ProcessedCommitsStore processedCommitsStore;
    protected final TargetLifecycleHooksResolver targetLifecycleHooksResolver;
    protected final EncryptionAwareConfigurationReader configurationReader;
    protected final UpgradeManager<Target> upgradeManager;
    protected final Set<Target> currentTargets;

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
            @Autowired TargetLifecycleHooksResolver targetLifecycleHooksResolver,
            @Autowired EncryptionAwareConfigurationReader configurationReader,
            @Autowired UpgradeManager<Target> upgradeManager) {
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
        this.configurationReader = configurationReader;
        this.upgradeManager = upgradeManager;
        this.currentTargets = new CopyOnWriteArraySet<>();
    }

    public void afterPropertiesSet() throws DeployerException {
        if (targetConfigFolder.exists()) {
            return;
        }
        logger.info("Target config folder '{}' doesn't exist. Creating it", targetConfigFolder);

        try {
            FileUtils.forceMkdir(targetConfigFolder);
        } catch (IOException e) {
            throw new DeployerException(format("Failed to create target config folder at '%s'", targetConfigFolder));
        }
    }

    @Override
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        // Load all existing targets on startup
        try {
            List<Target> targets = resolveTargets();
            if (CollectionUtils.isEmpty(targets)) {
                logger.warn("No config files found under '{}'", targetConfigFolder.getAbsolutePath());
            } else {
                // check if there are any targets that need to be unlocked
                targets.forEach(Target::unlock);
            }
        } catch (DeployerException e) {
            logger.error("Error while loading targets on startup", e);
        }
    }

    @Override
    public void destroy() {
        logger.info("Closing all targets...");

        if (isNotEmpty(currentTargets)) {
            currentTargets.forEach(Target::close);
        }
    }

    @Override
    public List<Target> getAllTargets() {
        return new ArrayList<>(currentTargets);
    }

    @Override
    public boolean targetExists(String env, String siteName) {
        String id = TargetImpl.getId(env, siteName);

        return findLoadedTargetById(id) != null;
    }

    @Override
    public Target getTarget(String env, String siteName) throws TargetNotFoundException {
        String id = TargetImpl.getId(env, siteName);
        Target target = findLoadedTargetById(id);

        if (target != null) {
            return target;
        }
        throw new TargetNotFoundException(id, env, siteName);
    }

    @Override
    public synchronized List<Target> resolveTargets() throws TargetServiceException {
        Collection<File> configFiles = getTargetConfigFiles();
        List<Target> targets = new ArrayList<>();

        if (!isNotEmpty(configFiles)) {
            return targets;
        }
        closeTargetsWithNoConfigFile(configFiles);

        for (File file : configFiles) {
            Target target = resolveTargetFromConfigFile(file, false);
            targets.add(target);
        }

        return targets;
    }

    @Override
    public synchronized Target createTarget(String env, String siteName, boolean replace, String templateName,
                                            Map<String, Object> templateParams)
            throws TargetAlreadyExistsException,
            TargetServiceException {
        String id = TargetImpl.getId(env, siteName);
        File configFile = new File(targetConfigFolder, id + "." + YAML_FILE_EXTENSION);

        if (!replace && configFile.exists()) {
            throw new TargetAlreadyExistsException(id, env, siteName);
        }
        createConfigFromTemplate(env, siteName, id, templateName, templateParams, configFile);

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

        cleanupTarget(id, target.getConfigurationFile());
    }

    private void cleanupTarget(String targetId, File configFile) throws TargetServiceException {
        try {
            processedCommitsStore.delete(targetId);
        } catch (DeployerException e) {
            throw new TargetServiceException(format("Error while deleting processed commit from store for target '%s'", targetId), e);
        }
        if (configFile.exists()) {
            logger.info("Deleting target configuration file at '{}'", configFile);

            FileUtils.deleteQuietly(configFile);
        }

        File contextFile = new File(targetConfigFolder, format(APPLICATION_CONTEXT_FILENAME_FORMAT,
                configFile.getName()));
        if (contextFile.exists()) {
            logger.info("Deleting target context file at '{}'", contextFile);

            FileUtils.deleteQuietly(contextFile);
        }
    }

    @Override
    public void recreateIndex(String env, String siteName) throws TargetNotFoundException {
        Target target = getTarget(env, siteName);
        ApplicationContext appContext = target.getApplicationContext();
        ElasticsearchAdminService adminService = appContext.getBean(ElasticsearchAdminService.class);
        adminService.recreateIndex(target.getId());
    }

    /**
     * Duplicates the index from the source site to the target site.
     *
     * @param sourceTarget the source site
     * @param siteName     the target site name
     */
    protected void duplicateIndex(final Target sourceTarget, final String siteName) {
        String indexIdFormat = sourceTarget.getConfiguration().getString("target.search.indexIdFormat");
        ApplicationContext appContext = sourceTarget.getApplicationContext();
        ElasticsearchAdminService adminService = appContext.getBean(ElasticsearchAdminService.class);
        adminService.duplicateIndex(format(indexIdFormat, sourceTarget.getSiteName()), format(indexIdFormat, siteName));
    }

    @Override
    public synchronized void duplicateTarget(final String env, final String sourceSiteName, final String siteName)
            throws TargetNotFoundException, TargetAlreadyExistsException, TargetServiceException {
        if (targetExists(env, siteName)) {
            throw new TargetAlreadyExistsException(siteName, env, siteName);
        }
        Target sourceTarget = getTarget(env, sourceSiteName);

        String newTargetId = TargetImpl.getId(env, siteName);
        Target target = null;
        try {
            // Create processed-commits file for the new target
            ObjectId processedCommit = processedCommitsStore.load(sourceTarget.getId());
            processedCommitsStore.store(newTargetId, processedCommit);

            // Create new target file from the source target
            target = duplicateTargetConfigurations(sourceTarget, siteName);

            // Duplicate search index
            duplicateIndex(sourceTarget, siteName);

            startInit(target);
            currentTargets.add(target);
        } catch (TargetAlreadyExistsException e) {
            logger.error("Failed to duplicate source target '{}' env '{}' into '{}'", sourceSiteName, env, siteName, e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to duplicate source target '{}' env '{}' into '{}'", sourceSiteName, env, siteName, e);
            try {
                if (target != null) {
                    target.delete();
                }
                File configFile = new File(targetConfigFolder, newTargetId + "." + YAML_FILE_EXTENSION);
                cleanupTarget(newTargetId, configFile);
            } catch (Exception ex) {
                logger.error("Failed to delete target '{}' env '{}' after failed duplication", sourceSiteName, env, ex);
            }
            throw new TargetServiceException(format("Failed to duplicate source target '%s' env '%s' into '%s'", sourceSiteName, env, siteName), e);
        }
    }

    /**
     * Creates a new target configuration based on an existing target.
     * This method will copy the TARGET-context.xml and TARGET.yaml files from the source target
     * and update the siteName and localRepoPath properties in the new TARGET.yaml file to
     * reflect the new siteName.
     *
     * @param sourceTarget the source target
     * @param siteName     the new site name
     * @throws TargetAlreadyExistsException if a target already exists for the new site name
     * @throws IOException                  if an error occurs while copying the source target configuration
     * @throws ConfigurationException       if an error occurs while reading the source target configuration
     */
    protected Target duplicateTargetConfigurations(Target sourceTarget, String siteName)
            throws TargetAlreadyExistsException, IOException, ConfigurationException {
        String newTargetId = TargetImpl.getId(sourceTarget.getEnv(), siteName);
        File configFile = new File(targetConfigFolder, newTargetId + "." + YAML_FILE_EXTENSION);
        if (configFile.exists()) {
            throw new TargetAlreadyExistsException(newTargetId, sourceTarget.getEnv(), siteName);
        }

        File sourceContextFile = new File(targetConfigFolder, format(APPLICATION_CONTEXT_FILENAME_FORMAT, sourceTarget.getId()));
        File contextFile = new File(targetConfigFolder, format(APPLICATION_CONTEXT_FILENAME_FORMAT, newTargetId));
        if (sourceContextFile.exists()) {
                FileUtils.copyFile(sourceContextFile, contextFile);
        }
        try {
            YAMLConfiguration targetConfiguration = new KeyOrderedYAMLConfiguration();
            try (InputStream is = Files.newInputStream(sourceTarget.getConfigurationFile().toPath())) {
                targetConfiguration.read(is);
            }
            targetConfiguration.setProperty(TARGET_SITE_NAME_CONFIG_KEY, siteName);
            String newRepoPath = targetConfiguration.getString(TARGET_LOCAL_REPO_CONFIG_KEY).replace(sourceTarget.getSiteName(), siteName);
            targetConfiguration.setProperty(TARGET_LOCAL_REPO_CONFIG_KEY, newRepoPath);
            try (Writer writer = Files.newBufferedWriter(configFile.toPath())) {
                targetConfiguration.write(writer);
            }
            return loadTarget(configFile, contextFile, true);
        } catch (Exception e) {
            throw new ConfigurationException(format("Failed to duplicate target configuration file '%s'", sourceTarget.getConfigurationFile()), e);
        }
    }

    protected Collection<File> getTargetConfigFiles() throws TargetServiceException {
        if (targetConfigFolder.exists()) {
            return FileUtils.listFiles(targetConfigFolder, new CustomConfigFileFilter(), null);
        }
        logger.warn("Config folder '{}' doesn't exist. Trying to create it...", targetConfigFolder.getAbsolutePath());

        try {
            FileUtils.forceMkdir(targetConfigFolder);
        } catch (IOException e) {
            throw new TargetServiceException(format("Unable to create config folder '%s'", targetConfigFolder), e);
        }

        return Collections.emptyList();
    }

    protected void closeTargetsWithNoConfigFile(Collection<File> configFiles) {
        if (!isNotEmpty(currentTargets)) {
            return;
        }
        currentTargets.removeIf(target -> {
            File configFile = target.getConfigurationFile();
            if (configFiles.contains(configFile)) {
                return false;
            }
            logger.info("Config file '{}' doesn't exist anymore for target '{}'. Closing target...",
                    configFile, target.getId());

            target.close();

            return true;
        });
    }

    protected Target resolveTargetFromConfigFile(File configFile, boolean create) throws TargetServiceException {
        String baseName = FilenameUtils.getBaseName(configFile.getName());
        File contextFile = new File(targetConfigFolder, format(APPLICATION_CONTEXT_FILENAME_FORMAT, baseName));
        Target target = findLoadedTargetByConfigFile(configFile);

        if (target != null) {
            // Check if the YAML config file or the app context file have changed since target load.
            long yamlLastModified = configFile.exists() ? configFile.lastModified() : 0;
            long contextLastModified = contextFile.exists() ? contextFile.lastModified() : 0;
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
    protected TargetImpl buildTarget(File configFile, File contextFile) throws Exception {
        HierarchicalConfiguration<ImmutableNode> config = loadConfiguration(configFile);
        String env = getRequiredStringProperty(config, TARGET_ENV_CONFIG_KEY);
        String siteName = getRequiredStringProperty(config, TARGET_SITE_NAME_CONFIG_KEY);
        String targetId = TargetImpl.getId(env, siteName);

        config.setProperty(TARGET_ID_CONFIG_KEY, targetId);
        config.setProperty(TARGET_CONFIG_PATH_KEY, configFile.toString());

        ConfigurableApplicationContext context = loadApplicationContext(config, contextFile);

        return context.getBean(TargetImpl.class);
    }

    protected Target loadTarget(File configFile, File contextFile, boolean create) throws TargetServiceException {
        try {
            // Create the target temporarily to run upgrades
            TargetImpl target = buildTarget(configFile, contextFile);
            upgradeManager.upgrade(target);
            target.close();

            // Create again with all upgrades applied
            target = buildTarget(configFile, contextFile);

            if (create) {
                executeCreateHooks(target);
            }

            startInit(target);

            return target;
        } catch (Exception e) {
            if (create) {
                FileUtils.deleteQuietly(configFile);
            }

            throw new TargetServiceException(format("Failed to load target for configuration file '%s'", configFile), e);
        }
    }

    protected HierarchicalConfiguration loadConfiguration(File configFile) throws ConfigurationException {
        String configFilename = configFile.getPath();

        logger.debug("Loading target YAML config at {}", configFilename);

        HierarchicalConfiguration config = configurationReader.readYamlConfiguration(configFile);

        if (!baseTargetYamlConfigResource.exists() && !baseTargetYamlConfigOverrideResource.exists()) {
            return config;
        }
        CombinedConfiguration combinedConfig = new CombinedConfiguration(new OverrideCombiner());

        combinedConfig.addConfiguration(config);
        combinedConfig.setPrefixLookups(config.getInterpolator().getLookups());

        if (baseTargetYamlConfigOverrideResource.exists()) {
            logger.debug("Loading base target YAML config override at {}", baseTargetYamlConfigOverrideResource);

            combinedConfig.addConfiguration(
                    configurationReader.readYamlConfiguration(baseTargetYamlConfigOverrideResource));
        }
        if (baseTargetYamlConfigResource.exists()) {
            logger.debug("Loading base target YAML config at {}", baseTargetYamlConfigResource);

            combinedConfig.addConfiguration(
                    configurationReader.readYamlConfiguration(baseTargetYamlConfigResource));
        }

        return combinedConfig;
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
                throw new ConfigurationException(format("Failed to load application context at '%s'", baseTargetContextResource),
                        e);
            }
        }
        if (baseTargetContextOverrideResource.exists()) {
            logger.debug("Loading base target application context override at {}", baseTargetContextOverrideResource);

            try {
                reader.loadBeanDefinitions(baseTargetContextOverrideResource);
            } catch (Exception e) {
                throw new ConfigurationException(format("Failed to load application context at '%s'", baseTargetContextOverrideResource), e);
            }
        }
        if (contextFile.exists()) {
            logger.debug("Loading target application context at '{}'", contextFile);

            try (InputStream in = new BufferedInputStream(new FileInputStream(contextFile))) {
                reader.loadBeanDefinitions(new InputSource(in));
            } catch (Exception e) {
                throw new ConfigurationException(format("Failed to load application context at '%s'", contextFile), e);
            }
        }

        context.refresh();

        return context;
    }

    protected void createConfigFromTemplate(String env, String siteName, String targetId, String templateName,
                                            Map<String, Object> templateParameters,
                                            File configFile) throws TargetServiceException {
        if (StringUtils.isEmpty(templateName)) {
            templateName = defaultTargetConfigTemplateName;
        }

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(TARGET_ENV_MODEL_KEY, env);
        templateModel.put(TARGET_SITE_NAME_MODEL_KEY, siteName);
        templateModel.put(TARGET_ID_MODEL_KEY, targetId);

        if (MapUtils.isNotEmpty(templateParameters)) {
            templateModel.putAll(templateParameters);
        }

        logger.info("Creating new target YAML configuration at {} using template '{}'", configFile, templateName);

        try (Writer out = new BufferedWriter(new FileWriter(configFile))) {
            processConfigTemplate(templateName, templateModel, out);

            out.flush();
        } catch (IOException e) {
            throw new TargetServiceException(format("Unable to open writer to YAML configuration file '%s'", configFile), e);
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
            throw new TargetServiceException(format("Processing of configuration template '%s' failed", templateName), e);
        }

        ValidationResult result = helper.getValidationResult();

        helper.clearValidationResult();

        if (result != null && result.hasErrors()) {
            throw new TargetServiceException(new ValidationException(result));
        }
    }

    protected Target findLoadedTargetByConfigFile(File configFile) {
        if (isEmpty(currentTargets)) {
            return null;
        }
        return currentTargets.stream()
                .filter(target -> target.getConfigurationFile().equals(configFile))
                .findFirst()
                .orElse(null);
    }

    protected Target findLoadedTargetById(String id) {
        if (isEmpty(currentTargets)) {
            return null;
        }
        return currentTargets.stream()
                .filter(target -> target.getId().equals(id))
                .findFirst()
                .orElse(null);
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
