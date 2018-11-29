/*
 * Copyright (C) 2007-2018 Crafter Software Corporation.
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
package org.craftercms.deployer.utils.core;

import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.craftercms.core.store.impl.filesystem.FileSystemContentStoreAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.PreDestroy;

/**
 * Factory for a singleton Core {@link Context}. The context is created on the first {@link #getObject()} call, and
 * destroyed when the factory is destroyed.
 *
 * <p>
 * This class is not thread safe and should only be called by a single deployment thread.
 * </p>
 *
 * @author avasquez
 */
public class SingletonContextFactory implements ObjectFactory<Context> {

    private static final Logger logger = LoggerFactory.getLogger(SingletonContextFactory.class);

    private String localRepoUrl;
    private ContentStoreService contentStoreService;
    private boolean xmlMergingEnabled;

    private Context context;

    @Required
    public void setLocalRepoUrl(String localRepoUrl) {
        this.localRepoUrl = localRepoUrl;
    }

    @Required
    public void setContentStoreService(ContentStoreService contentStoreService) {
        this.contentStoreService = contentStoreService;
    }

    @Required
    public void setXmlMergingEnabled(boolean xmlMergingEnabled) {
        this.xmlMergingEnabled = xmlMergingEnabled;
    }

    @Override
    public Context getObject() throws BeansException {
        if (context == null) {
            try {
                context = contentStoreService.createContext(FileSystemContentStoreAdapter.STORE_TYPE, null, null,
                                                            null, localRepoUrl, xmlMergingEnabled, false, 0,
                                                            Context.DEFAULT_IGNORE_HIDDEN_FILES);

                logger.debug("Content store context created: {}", context);
            } catch (Exception e) {
                throw new BeanCreationException("Unable to create context for content store @ " + localRepoUrl, e);
            }
        }

        return context;
    }

    @PreDestroy
    public void destroy() {
        if (context != null) {
            try {
                contentStoreService.destroyContext(context);

                logger.debug("Content store context destroyed: {}", context);
            } catch (Exception e) {
                logger.warn("Unable to destroy context " + context, e);
            }
        }
    }

}
