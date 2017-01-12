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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PreDestroy;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.spring.ApacheCommonsConfiguration2PropertySource;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetManager;
import org.craftercms.deployer.api.exceptions.DeploymentConfigurationException;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_ENVIRONMENT_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_ID_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_SCHEDULED_DEPLOYMENT_CRON_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_SITE_NAME_CONFIG_KEY;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
@Component("targetResolver")
public class TargetManagerImpl implements TargetManager {

    private static final Logger logger = LoggerFactory.getLogger(TargetManagerImpl.class);

    public static final String YAML_FILE_EXTENSION = "yaml";
    public static final String APPLICATION_CONTEXT_FILENAME_FORMAT = "%s-context.xml";
    public static final String CONFIG_PROPERTY_SOURCE_NAME = "deploymentConfig";

    public static final String TARGET_ID_MODEL_KEY = "targetId";
    public static final String SITE_NAME_MODEL_KEY = "siteName";
    public static final String ENVIRONMENT_MODEL_KEY = "env";

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
    protected Map<String, Target> targetCache;

    public TargetManagerImpl(
        @Value("${deployer.main.target.idPattern}") String targetIdPattern,
        @Value("${deployer.main.target.config.path}") String targetConfigPath,
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
        this.targetConfigFolder = new File(targetConfigPath);
        this.baseTargetYamlConfigResource = baseTargetYamlConfigResource;
        this.baseTargetYamlConfigOverrideResource = baseTargetYamlConfigOverrideResource;
        this.baseTargetContextResource = baseTargetContextResource;
        this.baseTargetContextOverrideResource = baseTargetContextOverrideResource;
        this.defaultTargetConfigTemplateName = defaultTargetConfigTemplateName;
        this.targetConfigTemplateEngine = targetConfigTemplateEngine;
        this.mainApplicationContext = mainApplicationContext;
        this.deploymentPipelineFactory = deploymentPipelineFactory;
        this.taskScheduler = taskScheduler;

        targetCache = new HashMap<>();
    }

    @PreDestroy
    public void destroy() {
        logger.info("Closing all targets...");

        targetCache.values().forEach(Target::close);
    }

    @Override
    public synchronized List<Target> getAllTargets() throws DeploymentException {
        Collection<File> configFiles = getCustomConfigFiles();
        List<Target> targets = Collections.emptyList();

        if (CollectionUtils.isNotEmpty(configFiles)) {
            closeTargetsWithNoCustomConfigFile(configFiles);

            targets = resolveTargetsFromCustomConfigFiles(configFiles);
        }

        return targets;
    }

    @Override
    public Target getTarget(String targetId) throws DeploymentException {
        return getTarget(targetId, false, null, null);
    }

    @Override
    public synchronized Target getTarget(String targetId, boolean create, String templateName,
                                         Map<String, Object> parameters) throws DeploymentException {
        Matcher targetIdMatcher = matchTargetId(targetId);
        String siteName = extractSiteNameFromTargetIdMatch(targetIdMatcher);
        String env = extractEnvironmentFromTargetIdMatch(targetIdMatcher);
        File configFile = getConfigFileFromTargetId(targetId);

        if (!configFile.exists()) {
            if (create) {
                createConfigFromTemplate(targetId, siteName, env, templateName, parameters, configFile);
            } else {
                return null;
            }
        }

        return getTarget(targetId, siteName, env, configFile);
    }

    @Override
    public synchronized boolean deleteTarget(String targetId) throws DeploymentException {
        File configFile = getConfigFileFromTargetId(targetId);

        if (configFile.exists()) {
            if (targetCache.containsKey(targetId)) {
                logger.info("Removing loaded target '{}' from cache", targetId);

                Target target = targetCache.remove(targetId);
                if (target != null) {
                    target.close();
                }
            }

            logger.info("Deleting custom target YAML configuration file at {}", configFile);

            FileUtils.deleteQuietly(configFile);

            File contextFile =  new File(targetConfigFolder, String.format(APPLICATION_CONTEXT_FILENAME_FORMAT, targetId));
            if (contextFile.exists()) {
                logger.info("Deleting custom context file at {}", contextFile);

                FileUtils.deleteQuietly(contextFile);
            }

            return true;
        }

        return false;
    }

    protected Collection<File> getCustomConfigFiles() throws DeploymentException {
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
            throw new DeploymentException("Error while retrieving YAML config files from " + targetConfigFolder, e);
        }
    }

    protected List<Target> resolveTargetsFromCustomConfigFiles(Collection<File> configFiles) throws DeploymentException {
        List<Target> targets = new ArrayList<>();

        // Get current contexts
        for (File file : configFiles) {
            String targetId = getTargetIdFromFilename(file);
            Matcher targetIdMatcher = matchTargetId(targetId);
            String siteName = extractSiteNameFromTargetIdMatch(targetIdMatcher);
            String env = extractEnvironmentFromTargetIdMatch(targetIdMatcher);

            Target target = getTarget(targetId, siteName, env, file);

            targets.add(target);
        }

        return targets;
    }

    protected void closeTargetsWithNoCustomConfigFile(Collection<File> configFiles) {
        targetCache.values().removeIf(target -> {
            String targetId = target.getId();
            if (!configFiles.contains(getConfigFileFromTargetId(targetId))) {
                logger.info("No YAML config file found for target '{}' under {}. Closing target...", targetId, targetConfigFolder);

                target.close();

                return true;
            } else {
                return false;
            }
        });
    }

    protected Target getTarget(String targetId, String siteName, String env, File customConfigFile) throws DeploymentException {
        MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, targetId);
        try {
            File customContextFile = getContextFileFromTargetId(targetId);
            Target target = null;

            if (targetCache.containsKey(targetId)) {
               target = targetCache.get(targetId);

                // Check if the YAML config file or the app context file have changed since target load.
                long yamlLastModified = customConfigFile.exists() ? customConfigFile.lastModified() : 0;
                long contextLastModified = customContextFile.exists()? customContextFile.lastModified() : 0;
                long targetOpenedDate = target.getLoadDate().toInstant().toEpochMilli();

                // Refresh if the files have been modified.
                if (yamlLastModified >= targetOpenedDate || contextLastModified >= targetOpenedDate) {
                    logger.info("Configuration files haven been updated for '{}'. The target will be reloaded.", targetId);

                    target.close();

                    target = null;
                }
            } else {
                logger.info("No current target found for '{}'. The new target will be loaded.", targetId);
            }

            if (target == null) {
                logger.info("Loading target with ID '{}'", targetId);

                target = createTargetContext(targetId, siteName, env, customConfigFile, customContextFile);

                targetCache.put(targetId, target);
            }

            return target;
        } catch (Exception e) {
            throw new DeploymentException("Error while resolving target '" + targetId + "'", e);
        } finally {
            MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
        }
    }

    protected Target createTargetContext(String targetId, String siteName, String env, File customConfigFile,
                                         File customContextFile) throws IOException, DeploymentException {
        HierarchicalConfiguration config = loadConfiguration(customConfigFile);
        ConfigurableApplicationContext context = loadApplicationContext(config, customContextFile);

        config.setProperty(TARGET_ID_CONFIG_KEY, targetId);
        config.setProperty(TARGET_SITE_NAME_CONFIG_KEY, siteName);
        config.setProperty(TARGET_ENVIRONMENT_CONFIG_KEY, env);

        Target target = new TargetImpl(targetId, getDeploymentPipeline(config, context), config, context);
        scheduleDeployment(target);

        return target;
    }

    protected HierarchicalConfiguration loadConfiguration(File customConfigFile) throws IOException, DeploymentException {
        String customConfigFilename = customConfigFile.getPath();

        logger.debug("Loading custom target YAML config at {}", customConfigFilename);

        HierarchicalConfiguration customConfig = ConfigurationUtils.loadYamlConfiguration(customConfigFile);

        if (baseTargetYamlConfigResource.exists() || baseTargetYamlConfigOverrideResource.exists()) {
            CombinedConfiguration combinedConfig = new CombinedConfiguration(new OverrideCombiner());

            combinedConfig.addConfiguration(customConfig);

            if (baseTargetYamlConfigOverrideResource.exists()) {
                logger.debug("Loading base target YAML config override at {}", baseTargetYamlConfigOverrideResource);

                combinedConfig.addConfiguration(ConfigurationUtils.loadYamlConfiguration(baseTargetYamlConfigOverrideResource));
            }
            if (baseTargetYamlConfigResource.exists()) {
                logger.debug("Loading base target YAML config at {}", baseTargetYamlConfigResource);

                combinedConfig.addConfiguration(ConfigurationUtils.loadYamlConfiguration(baseTargetYamlConfigResource));
            }

            return combinedConfig;
        } else {
            return customConfig;
        }
    }

    protected ConfigurableApplicationContext loadApplicationContext(HierarchicalConfiguration config,
                                                                    File customContextFile) throws IOException {
        GenericApplicationContext context = new GenericApplicationContext(mainApplicationContext);

        MutablePropertySources propertySources = context.getEnvironment().getPropertySources();
        propertySources.addFirst(new ApacheCommonsConfiguration2PropertySource(CONFIG_PROPERTY_SOURCE_NAME, config));

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);

        if (baseTargetContextResource.exists()) {
            logger.debug("Loading base target application context at {}", baseTargetContextResource);

            reader.loadBeanDefinitions(baseTargetContextResource);
        }
        if (baseTargetContextOverrideResource.exists()) {
            logger.debug("Loading base target application context override at {}", baseTargetContextResource);

            reader.loadBeanDefinitions(baseTargetContextResource);
        }
        if (customContextFile.exists()) {
            logger.debug("Loading custom target application context at {}", customContextFile);

            try (InputStream in = new BufferedInputStream(new FileInputStream(customContextFile))) {
                reader.loadBeanDefinitions(new InputSource(in));
            }
        }

        context.refresh();

        return context;
    }

    protected DeploymentPipeline getDeploymentPipeline(HierarchicalConfiguration config,
                                                       ApplicationContext appContext) throws DeploymentException {
        return deploymentPipelineFactory.getPipeline(config, appContext, TARGET_DEPLOYMENT_PIPELINE_CONFIG_KEY, true);
    }

    protected void scheduleDeployment(Target target) throws DeploymentConfigurationException {
        String cron = ConfigurationUtils.getString(target.getConfiguration(), TARGET_SCHEDULED_DEPLOYMENT_CRON_CONFIG_KEY);
        if (StringUtils.isNotEmpty(cron)) {
            logger.info("Deployment for target '{}' scheduled with cron {}", target.getId(), cron);

            target.scheduleDeployment(taskScheduler, cron);
        }
    }

    protected void createConfigFromTemplate(String targetId, String siteName, String env, String templateName,
                                            Map<String, Object> parameters, File configFile) throws DeploymentException {
        if (StringUtils.isEmpty(templateName)) {
            templateName = defaultTargetConfigTemplateName;
        }

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(TARGET_ID_MODEL_KEY, targetId);
        templateModel.put(SITE_NAME_MODEL_KEY, siteName);
        templateModel.put(ENVIRONMENT_MODEL_KEY, env);

        if (MapUtils.isNotEmpty(parameters)) {
            templateModel.putAll(parameters);
        }

        logger.info("Creating new custom target YAML configuration at {} using template '{}'", configFile, templateName);

        try (Writer out = new BufferedWriter(new FileWriter(configFile))) {
            processConfigTemplate(templateName, templateModel, out);
        } catch (IOException e) {
            throw new DeploymentException("Unable to open writer to YAML configuration file " + configFile, e);
        } catch (Exception e) {
            FileUtils.deleteQuietly(configFile);

            throw e;
        }
    }

    protected void processConfigTemplate(String templateName, Object templateModel, Writer out) throws DeploymentException {
        try {
            Template template = targetConfigTemplateEngine.compile(templateName);
            template.apply(templateModel, out);
        } catch (IOException e) {
            throw new DeploymentException("Processing of configuration template '" + templateName + "' failed", e);
        }
    }

    protected String getTargetIdFromFilename(File file) {
        return FilenameUtils.getBaseName(file.getName());
    }

    protected File getConfigFileFromTargetId(String targetId) {
        String filename = targetId + "." + YAML_FILE_EXTENSION;

        return new File(targetConfigFolder, filename);
    }

    protected File getContextFileFromTargetId(String targetId) {
        return new File(targetConfigFolder, String.format(APPLICATION_CONTEXT_FILENAME_FORMAT, targetId));
    }

    protected Matcher matchTargetId(String targetId) throws DeploymentConfigurationException {
        Matcher matcher = targetIdPattern.matcher(targetId);
        if (matcher.matches()) {
            return matcher;
        } else {
            throw new DeploymentConfigurationException("Target ID '" + targetId + "' doesn't match expected pattern " + targetIdPattern);
        }
    }

    protected String extractSiteNameFromTargetIdMatch(Matcher targetIdMatcher) {
        if (targetIdMatcher.groupCount() > 0) {
            return targetIdMatcher.group(1);
        } else {
            String targetId = targetIdMatcher.group();

            logger.warn("The match for target ID " + targetId + " using pattern " + targetIdPattern + " produced no capturing group. " +
                        "Please check the pattern and make sure to specify at least one capturing group to extract the site name");

            return targetId;
        }
    }

    protected String extractEnvironmentFromTargetIdMatch(Matcher targetIdMatcher) {
        if (targetIdMatcher.groupCount() > 1) {
            return targetIdMatcher.group(2);
        } else {
            String targetId = targetIdMatcher.group();

            logger.info("No environment could be extracted from target ID '" + targetId + "'");

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
