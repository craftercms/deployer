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
import org.craftercms.deployer.api.DeploymentContext;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.DeploymentResolver;
import org.craftercms.deployer.api.ErrorHandler;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.craftercms.deployer.utils.ConfigurationUtils;
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
import org.springframework.stereotype.Component;
import org.xml.sax.InputSource;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
@Component("deploymentResolver")
public class DeploymentResolverImpl implements DeploymentResolver {

    private static final Logger logger = LoggerFactory.getLogger(DeploymentResolverImpl.class);

    public static final String APP_CONTEXT_FILENAME_SUFFIX = "-context.xml";
    public static final String YAML_FILE_EXTENSION = "yaml";

    public static final String ERROR_HANDLER_BEAN_NAME = "errorHandler";

    protected File configFolder;
    protected Resource baseDeploymentYamlConfigResource;
    protected Resource baseDeploymentYamlConfigOverrideResource;
    protected Resource baseDeploymentAppContextResource;
    protected Resource baseDeploymentAppContextOverrideResource;
    protected final ApplicationContext mainApplicationContext;
    protected final DeploymentPipelineFactory deploymentPipelineFactory;
    protected Map<String, DeploymentContext> deploymentContextCache;

    public DeploymentResolverImpl(
        @Value("${deployer.configLocation}") Resource configResource,
        @Value("${deployer.baseDeploymentConfig.yamlLocation}") Resource baseDeploymentYamlConfigResource,
        @Value("${deployer.baseDeploymentConfig.yamlOverrideLocation}") Resource baseDeploymentYamlConfigOverrideResource,
        @Value("${deployer.baseDeploymentConfig.appContextLocation}") Resource baseDeploymentAppContextResource,
        @Value("${deployer.baseDeploymentConfig.appContextOverrideLocation}") Resource baseDeploymentAppContextOverrideResource,
        @Autowired ApplicationContext mainApplicationContext,
        @Autowired DeploymentPipelineFactory deploymentPipelineFactory) throws IOException {
        this.configFolder = configResource.getFile();
        this.baseDeploymentYamlConfigResource = baseDeploymentYamlConfigResource;
        this.baseDeploymentYamlConfigOverrideResource = baseDeploymentYamlConfigOverrideResource;
        this.baseDeploymentAppContextResource = baseDeploymentAppContextResource;
        this.baseDeploymentAppContextOverrideResource = baseDeploymentAppContextOverrideResource;
        this.mainApplicationContext = mainApplicationContext;
        this.deploymentPipelineFactory = deploymentPipelineFactory;

        deploymentContextCache = new HashMap<>();
    }

    @PreDestroy
    public void destroy() {
        logger.info("Destroying all deployment contexts...");

        deploymentContextCache.values().forEach(DeploymentContext::destroy);
    }

    @Override
    public List<DeploymentContext> resolveAll() throws DeploymentException {
        Collection<File> configFiles = getCustomConfigFiles();
        List<DeploymentContext> deploymentContexts = Collections.emptyList();

        if (CollectionUtils.isNotEmpty(configFiles)) {
            destroyContextsWithNoCustomConfigFile(configFiles);

            deploymentContexts = resolveContextsFromCustomConfigFile(configFiles);
        }


        return deploymentContexts;
    }

    protected Collection<File> getCustomConfigFiles() {
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

    protected String getDeploymentIdFromFilename(File file) {
        return FilenameUtils.getBaseName(file.getName());
    }

    protected File getConfigFileFromDeploymentId(String deploymentId) {
        String filename = deploymentId + "." + YAML_FILE_EXTENSION;

        return new File(configFolder, filename);
    }

    protected List<DeploymentContext> resolveContextsFromCustomConfigFile(Collection<File> configFiles) {
        List<DeploymentContext> deploymentContexts = new ArrayList<>();

        // Get current contexts
        configFiles.forEach(file -> {
            String deploymentId = getDeploymentIdFromFilename(file);
            DeploymentContext deploymentContext = getDeploymentContext(deploymentId, file);

            deploymentContexts.add(deploymentContext);
        });

        return deploymentContexts;
    }

    protected void destroyContextsWithNoCustomConfigFile(Collection<File> configFiles) {
        deploymentContextCache.values().removeIf(context -> {
            String contextId = context.getId();
            if (!configFiles.contains(getConfigFileFromDeploymentId(contextId))) {
                logger.info("No YAML config file found for deployment '{}' under {}. Destroying deployment context...",
                            contextId, configFolder);

                context.destroy();

                return true;
            } else {
                return false;
            }
        });
    }

    protected DeploymentContext getDeploymentContext(String deploymentId, File customConfigFile) {
        try {
            File customAppContextResource = new File(configFolder, deploymentId + APP_CONTEXT_FILENAME_SUFFIX);
            DeploymentContext deploymentContext = null;

            if (deploymentContextCache.containsKey(deploymentId)) {
               deploymentContext = deploymentContextCache.get(deploymentId);

                // Check if the YAML config file or the app context file have changed since the context was created.
                long yamlLastModified = customConfigFile.exists() ? customConfigFile.lastModified() : 0;
                long appContextLastModified = customAppContextResource.exists()? customAppContextResource.lastModified() : 0;
                long contextDateCreated = deploymentContext.getDateCreated();

                // Refresh if the files have been modified.
                if (yamlLastModified >= contextDateCreated || appContextLastModified >= contextDateCreated) {
                    logger.info("Configuration files haven been updated for '{}'. A new deployment context will be created.",
                                deploymentId);

                    deploymentContext.destroy();

                    deploymentContext = null;
                }
            } else {
                logger.info("No previous deployment context found for '{}'. A new one will be created.", deploymentId);
            }

            if (deploymentContext == null) {
                logger.info("Creating deployment context for '{}'", deploymentId);

                deploymentContext = createDeploymentContext(deploymentId, customConfigFile, customAppContextResource);

                deploymentContextCache.put(deploymentId, deploymentContext);
            }

            return deploymentContext;
        } catch (Exception e) {
            throw new DeploymentException("Error while resolving context for deployment '" + deploymentId + "'", e);
        }
    }

    protected DeploymentContext createDeploymentContext(String deploymentId, File customConfigFile,
                                                        File customAppContextFile) throws IOException {
        HierarchicalConfiguration config = loadConfiguration(customConfigFile);
        ConfigurableApplicationContext appContext = loadApplicationContext(config, customAppContextFile);

        DeploymentPipeline deploymentPipeline = getDeploymentPipeline(config, appContext);
        ErrorHandler errorHandler = getErrorHandler(appContext);

        return new DeploymentContextImpl(deploymentId, deploymentPipeline, errorHandler, appContext);
    }

    protected HierarchicalConfiguration loadConfiguration(File customConfigFile) throws IOException {
        logger.debug("Loading custom deployment YAML config at {}", customConfigFile);

        HierarchicalConfiguration customConfig = ConfigurationUtils.loadYamlConfiguration(customConfigFile);

        if (!baseDeploymentYamlConfigResource.exists() && !baseDeploymentYamlConfigOverrideResource.exists()) {
            CombinedConfiguration combinedConfig = new CombinedConfiguration(new OverrideCombiner());

            combinedConfig.addConfiguration(customConfig);

            if (baseDeploymentYamlConfigOverrideResource.exists()) {
                File configFile = baseDeploymentYamlConfigOverrideResource.getFile();

                logger.debug("Loading base deployment YAML config override at {}", baseDeploymentYamlConfigOverrideResource);

                combinedConfig.addConfiguration(ConfigurationUtils.loadYamlConfiguration(configFile));
            }
            if (baseDeploymentYamlConfigResource.exists()) {
                File configFile = baseDeploymentYamlConfigResource.getFile();

                logger.debug("Loading base deployment YAML config at {}", baseDeploymentYamlConfigOverrideResource);

                combinedConfig.addConfiguration(ConfigurationUtils.loadYamlConfiguration(configFile));
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
        propertySources.addFirst(new ApacheCommonsConfiguration2PropertySource("deploymentConfig", config));

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(appContext);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);

        if (baseDeploymentAppContextResource.exists()) {
            logger.debug("Loading base deployment application context at {}", baseDeploymentAppContextResource);

            reader.loadBeanDefinitions(baseDeploymentAppContextResource);
        }
        if (baseDeploymentAppContextOverrideResource.exists()) {
            logger.debug("Loading base deployment application context override at {}", baseDeploymentAppContextResource);

            reader.loadBeanDefinitions(baseDeploymentAppContextResource);
        }
        if (customDeploymentAppContextFile.exists()) {
            logger.debug("Loading custom deployment application context at {}", customDeploymentAppContextFile);

            try (InputStream in = new BufferedInputStream(new FileInputStream(customDeploymentAppContextFile))) {
                reader.loadBeanDefinitions(new InputSource(in));
            }
        }

        appContext.refresh();

        return appContext;
    }

    protected DeploymentPipeline getDeploymentPipeline(HierarchicalConfiguration config, ApplicationContext appContext) {
        return deploymentPipelineFactory.getPipeline(config, appContext);
    }

    protected ErrorHandler getErrorHandler(ApplicationContext appContext) {
        return appContext.getBean(ERROR_HANDLER_BEAN_NAME, ErrorHandler.class);
    }

}
