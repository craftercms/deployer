/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.deployer.utils.elasticsearch;

import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

/**
 * Holds the configuration for connecting to Elasticsearch, either a single or multiple clusters
 *
 * @author joseross
 * @since 3.1.5
 */
public class ElasticsearchConfig {

    public static final String CONFIG_KEY_GLOBAL_CLUSTER = "target.search.elasticsearch";

    public static final String CONFIG_KEY_READ_CLUSTER = "target.search.elasticsearch.readCluster";

    public static final String CONFIG_KEY_WRITE_CLUSTERS = "target.search.elasticsearch.writeClusters";

    /**
     * The global cluster, used for connecting to a single cluster for read & write operations
     */
    public final ElasticsearchClusterConfig globalCluster;

    /**
     * The read cluster, used for connecting to multiple clusters
     */
    public final ElasticsearchClusterConfig readCluster;

    /**
     * The write clusters, used for connecting to multiple clusters
     */
    public final List<ElasticsearchClusterConfig> writeClusters;

    public ElasticsearchConfig(HierarchicalConfiguration<?> config) {
        globalCluster = new ElasticsearchClusterConfig(config.configurationAt(CONFIG_KEY_GLOBAL_CLUSTER));
        readCluster = new ElasticsearchClusterConfig(config.configurationAt(CONFIG_KEY_READ_CLUSTER),
            globalCluster.username, globalCluster.password);
        writeClusters = config.configurationsAt(CONFIG_KEY_WRITE_CLUSTERS)
            .stream()
            .map(cluster -> new ElasticsearchClusterConfig(cluster, globalCluster.username, globalCluster.password))
            .collect(toList());
        if (useSingleCluster() && isEmpty(globalCluster.urls)) {
            throw new IllegalStateException("Invalid Elasticsearch configuration");
        }
    }

    /**
     * Indicates if a single cluster should be used, only if any of the read or write clusters is missing
     */
    public boolean useSingleCluster() {
        return isEmpty(readCluster.urls) || isEmpty(writeClusters);
    }

}
