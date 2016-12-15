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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PreDestroy;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.craftercms.deployer.api.Deployer;
import org.craftercms.deployer.api.EventListener;
import org.craftercms.deployer.api.SiteContext;
import org.craftercms.deployer.api.SiteResolver;
import org.craftercms.deployer.api.exception.DeploymentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
@Component("siteResolver")
public class SiteResolverImpl implements SiteResolver {

    private static final Logger logger = LoggerFactory.getLogger(SiteResolverImpl.class);

    public static final String SITE_APP_CONTEXT_FILENAME_SUFFIX = "-context.xml";

    public static final String SITE_NAME_PROPERTY_NAME = "siteName";

    @Value("${crafter.deployer.configLocation}")
    protected Resource configResource;
    @Value("${crafter.deployer.baseSiteContextLocation}")
    protected Resource baseSiteAppContextResource;
    @Value("${crafter.deployer.listeners.postDeployListenersBeanName}")
    protected String postDeployListenersBeanName;
    @Value("${crafter.deployer.listeners.errorListenersBeanName}")
    protected String errorListenersBeanName;
    @Autowired
    protected ApplicationContext mainApplicationContext;

    protected Map<String, SiteContext> siteContextCache;

    public SiteResolverImpl() {
        siteContextCache = new HashMap<>();
    }

    @PreDestroy
    public void destroy() {
        siteContextCache.values().forEach(SiteContext::close);
    }

    @Override
    public List<SiteContext> resolveAll() {
        Collection<File> propertiesFiles = getSitePropertiesFiles();
        List<SiteContext> siteContexts = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(propertiesFiles)) {
            propertiesFiles.forEach(file -> {
                String siteName = getSiteNameFromFilename(file);
                SiteContext siteContext = getSiteContext(siteName, file);

                siteContexts.add(siteContext);
            });
        }

        return siteContexts;
    }

    protected Collection<File> getSitePropertiesFiles() {
        File configFolder;
        try {
            configFolder = configResource.getFile();
        } catch (IOException e) {
            throw new DeploymentException("Unable to retrieve file from " + configResource, e);
        }

        return FileUtils.listFiles(configFolder, new String[] {"properties"}, false);
    }

    protected String getSiteNameFromFilename(File file) {
        return FilenameUtils.getBaseName(file.getName());
    }

    protected SiteContext getSiteContext(String siteName, File sitePropertiesFile) {
        try {
            Resource siteAppContextResource = configResource.createRelative(siteName + SITE_APP_CONTEXT_FILENAME_SUFFIX);
            SiteContext siteContext = null;

            if (siteContextCache.containsKey(siteName)) {
               siteContext = siteContextCache.get(siteName);

                // Check if the properties file or the app context file have changed since the site context was created.
                long sitePropertiesLastModified = sitePropertiesFile.lastModified();
                long siteAppContextLastModified = siteAppContextResource.lastModified();
                long siteContextDateCreated = siteContext.getDateCreated();

                // Refresh if the files have been modified.
                if (sitePropertiesLastModified >= siteContextDateCreated || siteAppContextLastModified >= siteContextDateCreated) {
                    logger.info("Configuration files haven been updated for '{}'. A new site context will be created.", siteName);

                    siteContext.close();

                    siteContext = null;
                }
            } else {
                logger.info("No previous site context found for '{}'. A new one will be created.", siteName);
            }

            if (siteContext == null) {
                logger.info("Creating site context for '{}'", siteName);

                siteContext = createSiteContext(siteName, sitePropertiesFile, siteAppContextResource);

                siteContextCache.put(siteName, siteContext);
            }

            return siteContext;
        } catch (Exception e) {
            throw new DeploymentException("Error while resolving context for site '" + siteName + "': " + e.getMessage(), e);
        }
    }

    protected SiteContext createSiteContext(String siteName, File sitePropertiesFile, Resource siteAppContextResource) {
        Properties siteProperties = loadSitePropertiesFile(siteName, sitePropertiesFile);
        ConfigurableApplicationContext siteAppContext = loadSiteAppContext(siteName, siteProperties, siteAppContextResource);
        Deployer deployer = getDeployer(siteAppContext);
        List<EventListener> postDeployListeners = getEvenListeners(siteAppContext, postDeployListenersBeanName);
        List<EventListener> errorListeners = getEvenListeners(siteAppContext, errorListenersBeanName);

        return new SiteContextImpl(siteName, deployer, postDeployListeners, errorListeners, siteAppContext);
    }

    protected Properties loadSitePropertiesFile(String siteName, File file) {
        logger.debug("Loading properties at {}", file);

        Properties properties = new Properties();
        try {
            properties.load(FileUtils.openInputStream(file));
        } catch (IOException e) {
            throw new DeploymentException("Unable to load properties at " + file, e);
        }

        properties.put(SITE_NAME_PROPERTY_NAME, siteName);

        return properties;
    }

    protected ConfigurableApplicationContext loadSiteAppContext(String siteName, Properties siteProperties,
                                                                Resource siteAppContextResource) {
        GenericApplicationContext siteAppContext = new GenericApplicationContext(mainApplicationContext);

        MutablePropertySources propertySources = siteAppContext.getEnvironment().getPropertySources();
        propertySources.addFirst(new PropertiesPropertySource(siteName + "Properties", siteProperties));

        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(siteAppContext);
        reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);

        if (baseSiteAppContextResource.exists()) {
            logger.debug("Loading base site application context at {}", baseSiteAppContextResource);

            reader.loadBeanDefinitions(baseSiteAppContextResource);
        }
        if (siteAppContextResource.exists()) {
            logger.debug("Loading custom site application context at {}", siteAppContextResource);

            reader.loadBeanDefinitions(siteAppContextResource);
        }

        siteAppContext.refresh();

        return siteAppContext;
    }

    protected Deployer getDeployer(ApplicationContext applicationContext) {
        return applicationContext.getBean(Deployer.class);
    }

    @SuppressWarnings("unchecked")
    protected List<EventListener> getEvenListeners(ApplicationContext applicationContext, String beanName) {
        return applicationContext.getBean(beanName, List.class);
    }

}
