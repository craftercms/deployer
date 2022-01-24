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

package org.craftercms.deployer.utils.elasticsearch.legacy;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.elasticsearch.client.RestHighLevelClient;

import static org.craftercms.search.elasticsearch.spring.RestHighLevelClientFactory.createClient;

/**
 * Holds the configuration for a single Elasticsearch cluster
 *
 * @author joseross
 * @since 3.1.5
 */
public class ElasticsearchClusterConfig {

    public static final String CONFIG_KEY_URLS = "urls";

    public static final String CONFIG_KEY_USERNAME = "username";

    public static final String CONFIG_KEY_PASSWORD = "password";

    public static final String CONFIG_KEY_TIMEOUT_CONNECT = "timeout.connect";

    public static final String CONFIG_KEY_TIMEOUT_SOCKET = "timeout.socket";

    public static final String CONFIG_KEY_THREADS = "threads";

    public static final String CONFIG_KEY_KEEP_ALIVE = "keepAlive";

    /**
     * The list of urls to connect to the cluster
     */
    public final String[] urls;

    /**
     * The username to connect to the cluster
     */
    public final String username;

    /**
     * The password to connect to the cluster
     */
    public final String password;

    public final int connectTimeout;

    public final int socketTimeout;

    public final int threadCount;

    public final boolean keepAlive;

    public ElasticsearchClusterConfig() {
        urls = null;
        username = null;
        password = null;
        connectTimeout = -1;
        socketTimeout = -1;
        threadCount = -1;
        keepAlive = false;
    }

    public ElasticsearchClusterConfig(HierarchicalConfiguration<?> config) {
        urls = (String[]) config.getArray(String.class, CONFIG_KEY_URLS);
        username = config.getString(CONFIG_KEY_USERNAME, null);
        password = config.getString(CONFIG_KEY_PASSWORD, null);
        connectTimeout = config.getInt(CONFIG_KEY_TIMEOUT_CONNECT, -1);
        socketTimeout = config.getInt(CONFIG_KEY_TIMEOUT_SOCKET, -1);
        threadCount = config.getInt(CONFIG_KEY_THREADS, -1);
        keepAlive = config.getBoolean(CONFIG_KEY_KEEP_ALIVE, false);
    }

    public ElasticsearchClusterConfig(HierarchicalConfiguration<?> config, String username, String password,
                                      int connectTimeout, int socketTimeout, int threadCount, boolean keepAlive) {
        urls = (String[]) config.getArray(String.class, CONFIG_KEY_URLS);
        this.username = config.getString(CONFIG_KEY_USERNAME, username);
        this.password = config.getString(CONFIG_KEY_PASSWORD, password);
        this.connectTimeout = config.getInt(CONFIG_KEY_TIMEOUT_CONNECT, connectTimeout);
        this.socketTimeout = config.getInt(CONFIG_KEY_TIMEOUT_SOCKET, socketTimeout);
        this.threadCount = config.getInt(CONFIG_KEY_THREADS, threadCount);
        this.keepAlive = config.getBoolean(CONFIG_KEY_KEEP_ALIVE, keepAlive);
    }

    /**
     * Returns a client matching the current configuration of the cluster
     */
    public RestHighLevelClient buildClient() {
        return createClient(urls, username, password, connectTimeout, socketTimeout ,threadCount, keepAlive);
    }

}
