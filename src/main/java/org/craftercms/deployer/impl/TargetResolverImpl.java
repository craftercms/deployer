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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PreDestroy;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.craftercms.commons.spring.ApacheCommonsConfiguration2PropertySource;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.TargetContext;
import org.craftercms.deployer.api.TargetResolver;
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
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

import static org.craftercms.deployer.impl.DeploymentConstants.DEPLOYMENT_PIPELINE_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.TARGET_ID_CONFIG_KEY;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
@Component("targetResolver")
public class TargetResolverImpl implements TargetResolver {

    private static final Logger logger = LoggerFactory.getLogger(TargetResolverImpl.class);

    public static final String APP_CONTEXT_FILENAME_SUFFIX = "-context.xml";
    public static final String YAML_FILE_EXTENSION = "yaml";
    public static final String CONFIG_PROPERTY_SOURCE_NAME = "deploymentConfig";

    protected File configFolder;
    protected Resource baseTargetYamlConfigResource;
    protected Resource baseTargetYamlConfigOverrideResource;
    protected Resource baseTargetAppContextResource;
    protected Resource baseTargetAppContextOverrideResource;
    protected final ApplicationContext mainApplicationContext;
    protected final DeploymentPipelineFactory deploymentPipelineFactory;
    protected Map<String, TargetContext> targetContextCache;

    public TargetResolverImpl(
        @Value("${deployer.main.config.path}") String configPath,
        @Value("${deployer.main.config.target.base.yamlLocation}") Resource baseTargetYamlConfigResource,
        @Value("${deployer.main.config.target.base.yamlOverrideLocation}") Resource baseTargetYamlConfigOverrideResource,
        @Value("${deployer.main.config.target.base.appContextLocation}") Resource baseTargetAppContextResource,
        @Value("${deployer.main.config.target.base.appContextOverrideLocation}") Resource baseTargetAppContextOverrideResource,
        @Autowired ApplicationContext mainApplicationContext,
        @Autowired DeploymentPipelineFactory deploymentPipelineFactory) throws IOException {
        this.configFolder = new File(configPath);
        this.baseTargetYamlConfigResource = baseTargetYamlConfigResource;
        this.baseTargetYamlConfigOverrideResource = baseTargetYamlConfigOverrideResource;
        this.baseTargetAppContextResource = baseTargetAppContextResource;
        this.baseTargetAppContextOverrideResource = baseTargetAppContextOverrideResource;
        this.mainApplicationContext = mainApplicationContext;
        this.deploymentPipelineFactory = deploymentPipelineFactory;

        targetContextCache = new HashMap<>();
    }

    @PreDestroy
    public void destroy() {
        logger.info("Destroying all deployment contexts...");

        targetContextCache.values().forEach(TargetContext::destroy);
    }

    @Override
    public List<TargetContext> resolveAll() throws DeploymentException {
        Collection<File> configFiles = getCustomConfigFiles();
        List<TargetContext> targetContexts = Collections.emptyList();

        if (CollectionUtils.isNotEmpty(configFiles)) {
            destroyContextsWithNoCustomConfigFile(configFiles);

            targetContexts = resolveContextsFromCustomConfigFiles(configFiles);
        }

        return targetContexts;
    }

    protected Collection<File> getCustomConfigFiles() throws DeploymentException {
        try {
            if (configFolder.exists()) {
                Collection<File> yamlFiles = FileUtils.listFiles(configFolder, new String[] {YAML_FILE_EXTENSION}, false);
                
                if (CollectionUtils.isEmpty(yamlFiles)) {
                    logger.warn("No YAML config files found under {}", configFolder.getAbsolutePath());
                }
                
                return yamlFiles;
            } else {
                logger.warn("Config folder {} doesn't exist", configFolder.getAbsolutePath());

                return Collections.emptyList();
            }
        } catch (Exception e) {
            throw new DeploymentException("Error while retrieving YAML config files from " + configFolder, e);
        }
    }

    protected String getTargetIdFromFilename(File file) {
        return FilenameUtils.getBaseName(file.getName());
    }

    protected File getConfigFileFromTargetId(String deploymentId) {
        String filename = deploymentId + "." + YAML_FILE_EXTENSION;

        return new File(configFolder, filename);
    }

    protected List<TargetContext> resolveContextsFromCustomConfigFiles(Collection<File> configFiles) throws DeploymentException {
        List<TargetContext> targetContexts = new ArrayList<>();

        // Get current contexts
        for (File file : configFiles) {
            String targetId = getTargetIdFromFilename(file);
            TargetContext targetContext = getTargetContext(targetId, file);

            targetContexts.add(targetContext);
        }

        return targetContexts;
    }

    protected void destroyContextsWithNoCustomConfigFile(Collection<File> configFiles) {
        targetContextCache.values().removeIf(context -> {
            String contextId = context.getId();
            if (!configFiles.contains(getConfigFileFromTargetId(contextId))) {
                logger.info("No YAML config file found for target '{}' under {}. Destroying target context...", contextId, configFolder);

                context.destroy();

                return true;
            } else {
                return false;
            }
        });
    }

    protected TargetContext getTargetContext(String targetId, File customConfigFile) throws DeploymentException {
        MDC.put(DeploymentConstants.TARGET_ID_MDC_KEY, targetId);
        try {
            File customAppContextResource = new File(configFolder, targetId + APP_CONTEXT_FILENAME_SUFFIX);
            TargetContext targetContext = null;

            if (targetContextCache.containsKey(targetId)) {
               targetContext = targetContextCache.get(targetId);

                // Check if the YAML config file or the app context file have changed since the context was created.
                long yamlLastModified = customConfigFile.exists() ? customConfigFile.lastModified() : 0;
                long appContextLastModified = customAppContextResource.exists()? customAppContextResource.lastModified() : 0;
                long contextDateCreated = targetContext.getDateCreated().toInstant().toEpochMilli();

                // Refresh if the files have been modified.
                if (yamlLastModified >= contextDateCreated || appContextLastModified >= contextDateCreated) {
                    logger.info("Configuration files haven been updated for '{}'. A new target context will be created.", targetId);

                    targetContext.destroy();

                    targetContext = null;
                }
            } else {
                logger.info("No previous target context found for '{}'. A new one will be created.", targetId);
            }

            if (targetContext == null) {
                logger.info("Creating target context for '{}'", targetId);

                targetContext = createTargetContext(targetId, customConfigFile, customAppContextResource);

                targetContextCache.put(targetId, targetContext);
            }

            return targetContext;
        } catch (Exception e) {
            throw new DeploymentException("Error while resolving context for target '" + targetId + "'", e);
        } finally {
            MDC.remove(DeploymentConstants.TARGET_ID_MDC_KEY);
        }
    }

    protected TargetContext createTargetContext(String targetId, File customConfigFile,
                                                File customAppContextFile) throws IOException, DeploymentException {
        HierarchicalConfiguration config = loadConfiguration(customConfigFile);
        ConfigurableApplicationContext appContext = loadApplicationContext(config, customAppContextFile);

        config.setProperty(TARGET_ID_CONFIG_KEY, targetId);

        DeploymentPipeline deploymentPipeline = getDeploymentPipeline(config, appContext);

        return new TargetContextImpl(targetId, deploymentPipeline, appContext);
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
                                                                    File customDeploymentAppContextFile) throws IOException {
        GenericApplicationContext appContext = new GenericApplicationContext(mainApplicationContext);

        MutablePropertySources propertySources = appContext.getEnvironment().getPropertySources();
        propertySources.addFirst(new ApacheCommonsConfiguration2PropertySource(CONFIG_PROPERTY_SOURCE_NAME, config));

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);

        if (baseTargetAppContextResource.exists()) {
            logger.debug("Loading base target application context at {}", baseTargetAppContextResource);

            reader.loadBeanDefinitions(baseTargetAppContextResource);
        }
        if (baseTargetAppContextOverrideResource.exists()) {
            logger.debug("Loading base target application context override at {}", baseTargetAppContextResource);

            reader.loadBeanDefinitions(baseTargetAppContextResource);
        }
        if (customDeploymentAppContextFile.exists()) {
            logger.debug("Loading custom target application context at {}", customDeploymentAppContextFile);

            try (InputStream in = new BufferedInputStream(new FileInputStream(customDeploymentAppContextFile))) {
                reader.loadBeanDefinitions(new InputSource(in));
            }
        }

        appContext.refresh();

        return appContext;
    }

    protected DeploymentPipeline getDeploymentPipeline(HierarchicalConfiguration config,
                                                       ApplicationContext appContext) throws DeploymentException {
        return deploymentPipelineFactory.getPipeline(config, appContext, DEPLOYMENT_PIPELINE_CONFIG_KEY, true);
    }

}
