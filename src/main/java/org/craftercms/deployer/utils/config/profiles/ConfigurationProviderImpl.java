/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.utils.config.profiles;

import org.craftercms.commons.config.ConfigurationProvider;
import org.craftercms.core.service.ContentStoreService;
import org.craftercms.core.service.Context;
import org.springframework.beans.factory.ObjectFactory;

import java.beans.ConstructorProperties;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Implementation of {@link ConfigurationProvider}
 *
 * @author joseross
 * @since 3.1.6
 */
public class ConfigurationProviderImpl implements ConfigurationProvider {

    protected ObjectFactory<Context> contextFactory;

    protected ContentStoreService contentStoreService;

    protected String siteName;

    @ConstructorProperties({"contextFactory", "contentStoreService", "siteName"})
    public ConfigurationProviderImpl(ObjectFactory<Context> contextFactory, ContentStoreService contentStoreService, String siteName) {
        this.contextFactory = contextFactory;
        this.contentStoreService = contentStoreService;
        this.siteName = siteName;
    }

    @Override
    public boolean configExists(String path) {
        return contentStoreService.exists(contextFactory.getObject(), path);
    }

    @Override
    public InputStream getConfig(String path) throws IOException {
        return contentStoreService.getContent(contextFactory.getObject(), path).getInputStream();
    }

    @Override
    public Map<String, String> getLookupVariables() {
        return contextFactory.getObject().getConfigLookupVariables();
    }
}
