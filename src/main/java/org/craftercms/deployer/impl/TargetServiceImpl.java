/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.craftercms.commons.spring.ApacheCommonsConfiguration2PropertySource;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.utils.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_DEPLOYMENT_PIPELINE_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_ID_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_SCHEDULED_DEPLOYMENT_CRON_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_SCHEDULED_DEPLOYMENT_ENABLED_CONFIG_KEY;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
@Component("targetService")
public class TargetServiceImpl implements TargetService {

    private static final Logger logger = LoggerFactory.getLogger(TargetServiceImpl.class);

    public static final String YAML_FILE_EXTENSION = "yaml";
    public static final String APPLICATION_CONTEXT_FILENAME_FORMAT = "%s-context.xml";
    public static final String CONFIG_PROPERTY_SOURCE_NAME = "targetConfig";

    public static final String TARGET_ID_MODEL_KEY = "target_id";

    protected Pattern targetIdPattern;
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
    protected Set<Target> targets;

    public TargetServiceImpl(
        @Value("${deployer.main.target.idPattern}") String targetIdPattern,
        @Value("${deployer.main.target.config.folderPath}") File targetConfigFolder,
        @Value("${deployer.main.target.config.baseYaml.location}") Resource baseTargetYamlConfigResource,
        @Value("${deployer.main.target.config.baseYaml.overrideLocation}") Resource baseTargetYamlConfigOverrideResource,
        @Value("${deployer.main.target.config.baseContext.location}") Resource baseTargetContextResource,
        @Value("${deployer.main.target.config.baseContext.overrideLocation}") Resource baseTargetContextOverrideResource,
        @Value("${deployer.main.target.config.templates.default}") String defaultTargetConfigTemplateName,
        @Autowired Handlebars targetConfigTemplateEngine,
        @Autowired ApplicationContext mainApplicationContext,
        @Autowired DeploymentPipelineFactory deploymentPipelineFactory,
        @Autowired TaskScheduler taskScheduler) throws IOException {
        this.targetIdPattern = Pattern.compile(targetIdPattern);
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
        this.targets = new HashSet<>();
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

    @PreDestroy
    public void destroy() {
        logger.info("Closing all targets...");

        if (CollectionUtils.isNotEmpty(targets)) {
            targets.forEach(Target::close);
        }
    }

    @Override
    public synchronized List<Target> getAllTargets() throws DeployerException {
        Collection<File> configFiles = getTargetConfigFiles();
        List<Target> targets = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(configFiles)) {
            closeTargetsWithNoConfigFile(configFiles);

            for (File file : configFiles) {
                Target target = resolveTargetFromConfigFile(file);
                targets.add(target);
            }
        }

        return targets;
    }

    @Override
    public synchronized Target getTarget(String id) throws DeployerException {
        Target target = findLoadedTargetById(id);
        if (target != null) {
            return target;
        } else {
            throw new TargetNotFoundException(id);
        }
    }

    @Override
    public synchronized Target createTarget(String id, boolean replace, String templateName,
                                            Map<String, Object> templateParameters) throws DeployerException {
        File configFile = new File(targetConfigFolder, id + "." + YAML_FILE_EXTENSION);
        if (!replace && configFile.exists()) {
            throw new TargetAlreadyExistsException(id);
        } else {
            createConfigFromTemplate(id, templateName, templateParameters, configFile);
        }

        return resolveTargetFromConfigFile(configFile);
    }

    @Override
    public synchronized void deleteTarget(String id) throws DeployerException {
        Target target = getTarget(id);
        target.close();

        logger.info("Removing loaded target '{}'", id);

        targets.remove(target);

        File configFile =  target.getConfigurationFile();
        if (configFile.exists()) {
            logger.info("Deleting target configuration file at {}", configFile);

            FileUtils.deleteQuietly(configFile);
        }

        File contextFile = new File(targetConfigFolder, String.format(APPLICATION_CONTEXT_FILENAME_FORMAT, configFile.getName()));
        if (contextFile.exists()) {
            logger.info("Deleting target context file at {}", contextFile);

            FileUtils.deleteQuietly(contextFile);
        }
    }

    protected Collection<File> getTargetConfigFiles() throws DeployerException {
        try {
            if (targetConfigFolder.exists()) {
                Collection<File> yamlFiles = FileUtils.listFiles(targetConfigFolder, new CustomConfigFileFilter(), null);
                
                if (CollectionUtils.isEmpty(yamlFiles)) {
                    logger.warn("No YAML config files found under {}", targetConfigFolder.getAbsolutePath());
                }
                
                return yamlFiles;
            } else {
                logger.warn("Config folder {} doesn't exist", targetConfigFolder.getAbsolutePath());

                return Collections.emptyList();
            }
        } catch (Exception e) {
            throw new DeployerException("Error while retrieving YAML config files from " + targetConfigFolder, e);
        }
    }

    protected void closeTargetsWithNoConfigFile(Collection<File> configFiles) {
        if (CollectionUtils.isNotEmpty(targets)) {
            targets.removeIf(target -> {
                File configFile = target.getConfigurationFile();
                if (!configFiles.contains(configFile)) {
                    logger.info("Config file {} doesn't exist anymore for target '{}'. Closing target...", configFile);

                    target.close();

                    return true;
                } else {
                    return false;
                }
            });
        }
    }

    protected Target resolveTargetFromConfigFile(File configFile) throws DeployerException {
        try {
            File contextFile = new File(targetConfigFolder, String.format(APPLICATION_CONTEXT_FILENAME_FORMAT, configFile.getName()));
            Target target = findLoadedTargetByConfigFile(configFile);

            if (target != null) {
                // Check if the YAML config file or the app context file have changed since target load.
                long yamlLastModified = configFile.exists() ? configFile.lastModified() : 0;
                long contextLastModified = contextFile.exists()? contextFile.lastModified() : 0;
                long targetOpenedDate = target.getLoadDate().toInstant().toEpochMilli();

                // Refresh if the files have been modified.
                if (yamlLastModified >= targetOpenedDate || contextLastModified >= targetOpenedDate) {
                    logger.info("Configuration files haven been updated for '{}'. The target will be reloaded.", target.getId());

                    target.close();

                    targets.remove(target);

                    target = null;
                }
            } else {
                logger.info("No loaded target found for configuration file '{}'", configFile);
            }

            if (target == null) {
                logger.info("Loading target for configuration file '{}'", configFile);

                target = createTarget(configFile, contextFile);
                targets.add(target);
            }

            return target;
        } catch (Exception e) {
            throw new DeployerException("Error while trying to resolve target from configuration file " + configFile, e);
        }
    }

    protected Target createTarget(File configFile, File contextFile) throws DeployerException {
        HierarchicalConfiguration config = loadConfiguration(configFile);
        ConfigurableApplicationContext context = loadApplicationContext(config, contextFile);
        String targetId = getTargetIdFromConfig(config);

        Target target = new TargetImpl(targetId, getDeploymentPipeline(config, context), configFile, config, context);
        scheduleDeployment(target);

        return target;
    }

    protected HierarchicalConfiguration loadConfiguration(File configFile) throws DeployerException {
        String configFilename = configFile.getPath();

        logger.debug("Loading target YAML config at {}", configFilename);

        HierarchicalConfiguration config = ConfigUtils.loadYamlConfiguration(configFile);

        if (baseTargetYamlConfigResource.exists() || baseTargetYamlConfigOverrideResource.exists()) {
            CombinedConfiguration combinedConfig = new CombinedConfiguration(new OverrideCombiner());

            combinedConfig.addConfiguration(config);

            if (baseTargetYamlConfigOverrideResource.exists()) {
                logger.debug("Loading base target YAML config override at {}", baseTargetYamlConfigOverrideResource);

                combinedConfig.addConfiguration(ConfigUtils.loadYamlConfiguration(baseTargetYamlConfigOverrideResource));
            }
            if (baseTargetYamlConfigResource.exists()) {
                logger.debug("Loading base target YAML config at {}", baseTargetYamlConfigResource);

                combinedConfig.addConfiguration(ConfigUtils.loadYamlConfiguration(baseTargetYamlConfigResource));
            }

            return combinedConfig;
        } else {
            return config;
        }
    }

    protected ConfigurableApplicationContext loadApplicationContext(HierarchicalConfiguration config,
                                                                    File contextFile) throws DeployerException {
        GenericApplicationContext context = new GenericApplicationContext(mainApplicationContext);

        MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        propertySources.addFirst(new ApacheCommonsConfiguration2PropertySource(CONFIG_PROPERTY_SOURCE_NAME, config));

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);

        if (baseTargetContextResource.exists()) {
            logger.debug("Loading base target application context at {}", baseTargetContextResource);

            try {
                reader.loadBeanDefinitions(baseTargetContextResource);
            } catch (Exception e) {
                throw new DeployerConfigurationException("Failed to load application context at " + baseTargetContextResource, e);
            }
        }
        if (baseTargetContextOverrideResource.exists()) {
            logger.debug("Loading base target application context override at {}", baseTargetContextOverrideResource);

            try {
                reader.loadBeanDefinitions(baseTargetContextOverrideResource);
            } catch (Exception e) {
                throw new DeployerConfigurationException("Failed to load application context at " + baseTargetContextOverrideResource, e);
            }
        }
        if (contextFile.exists()) {
            logger.debug("Loading target application context at {}", contextFile);

            try (InputStream in = new BufferedInputStream(new FileInputStream(contextFile))) {
                reader.loadBeanDefinitions(new InputSource(in));
            } catch (Exception e) {
                throw new DeployerConfigurationException("Failed to load application context at " + contextFile, e);
            }
        }

        context.refresh();

        return context;
    }

    protected String getTargetIdFromConfig(Configuration config) throws DeployerConfigurationException {
        return ConfigUtils.getRequiredStringProperty(config, TARGET_ID_CONFIG_KEY);
    }

    protected DeploymentPipeline getDeploymentPipeline(HierarchicalConfiguration config,
                                                       ConfigurableApplicationContext appContext) throws DeployerException {
        return deploymentPipelineFactory.getPipeline(config, appContext, TARGET_DEPLOYMENT_PIPELINE_CONFIG_KEY);
    }

    protected void scheduleDeployment(Target target) throws DeployerConfigurationException {
        boolean enabled =  ConfigUtils.getBooleanProperty(target.getConfiguration(), TARGET_SCHEDULED_DEPLOYMENT_ENABLED_CONFIG_KEY, true);
        String cron = ConfigUtils.getStringProperty(target.getConfiguration(), TARGET_SCHEDULED_DEPLOYMENT_CRON_CONFIG_KEY);

        if (enabled && StringUtils.isNotEmpty(cron)) {
            logger.info("Deployment for target '{}' scheduled with cron {}", target.getId(), cron);

            target.scheduleDeployment(taskScheduler, cron);
        }
    }

    protected void createConfigFromTemplate(String targetId, String templateName, Map<String, Object> templateParameters,
                                            File configFile) throws DeployerException {
        if (StringUtils.isEmpty(templateName)) {
            templateName = defaultTargetConfigTemplateName;
        }

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(TARGET_ID_MODEL_KEY, targetId);

        if (MapUtils.isNotEmpty(templateParameters)) {
            templateModel.putAll(templateParameters);
        }

        logger.info("Creating new target YAML configuration at {} using template '{}'", configFile, templateName);

        try (Writer out = new BufferedWriter(new FileWriter(configFile))) {
            processConfigTemplate(templateName, templateModel, out);

            out.flush();
        } catch (IOException e) {
            throw new DeployerException("Unable to open writer to YAML configuration file " + configFile, e);
        } catch (Exception e) {
            FileUtils.deleteQuietly(configFile);

            throw e;
        }
    }

    protected void processConfigTemplate(String templateName, Object templateModel, Writer out) throws DeployerException {
        try {
            Template template = targetConfigTemplateEngine.compile(templateName);
            template.apply(templateModel, out);
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            if (cause instanceof DeployerException) {
                throw (DeployerException)cause;
            } else {
                throw new DeployerException("Processing of configuration template '" + templateName + "' failed", e);
            }
        }
    }

    protected Target findLoadedTargetByConfigFile(File configFile) {
        if (CollectionUtils.isNotEmpty(targets)) {
            return targets.stream().filter(target -> target.getConfigurationFile().equals(configFile)).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    protected Target findLoadedTargetById(String targetId) {
        if (CollectionUtils.isNotEmpty(targets)) {
            return targets.stream().filter(target -> target.getId().equals(targetId)).findFirst().orElse(null);
        } else {
            return null;
        }
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
