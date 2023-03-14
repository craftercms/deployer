/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.deployer.utils.opensearch;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Base implementation for factories capable of build single or multi-cluster OpenSearch services
 *
 * @author joseross
 * @since 3.1.5
 */
public abstract class AbstractOpenSearchFactory<T> extends AbstractFactoryBean<T>
        implements BeanNameAware {

    private static final Logger logger = LoggerFactory.getLogger(AbstractOpenSearchFactory.class);

    /**
     * The name of the bean
     */
    protected String name;

    /**
     * The OpenSearch configuration
     */
    protected OpenSearchConfig config;

    public AbstractOpenSearchFactory(final OpenSearchConfig config) {
        this.config = config;
    }

    @Override
    public void setBeanName(final String name) {
        this.name = name;
    }

    @Override
    protected T createInstance() {
        logger.debug("Creating instance for '{}'", name);
        if (config.useSingleCluster()) {
            logger.debug("Using a single cluster configuration for '{}'", name);
            return doCreateSingleInstance(config.globalCluster.buildClient());
        }

        logger.debug("Using a multi-cluster configuration for '{}'", name);
        OpenSearchClient readClient = config.readCluster.buildClient();
        OpenSearchClient[] writeClients = config.writeClusters.stream()
                .map(OpenSearchClusterConfig::buildClient)
                .toArray(OpenSearchClient[]::new);
        return doCreateMultiInstance(readClient, writeClients);
    }

    /**
     * Creates a service instance for a single cluster
     *
     * @param client the OpenSearch client
     * @return the service instance
     */
    protected abstract T doCreateSingleInstance(OpenSearchClient client);

    /**
     * Creates a service instance for a multiple cluster
     *
     * @param readClient   the OpenSearch client for read-related operations
     * @param writeClients the OpenSearch clients for write-related operations
     * @return the service instance
     */
    protected abstract T doCreateMultiInstance(OpenSearchClient readClient, OpenSearchClient[] writeClients);

}
