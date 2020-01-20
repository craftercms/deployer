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

import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;

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

    /**
     * The list of urls to connect to the cluster
     */
    public final List<String> urls;

    /**
     * The username to connect to the cluster
     */
    public final String username;

    /**
     * The password to connect to the cluster
     */
    public final String password;

    public ElasticsearchClusterConfig() {
        urls = Collections.emptyList();
        username = null;
        password = null;
    }

    public ElasticsearchClusterConfig(HierarchicalConfiguration<?> config) {
        urls = config.getList(String.class, CONFIG_KEY_URLS);
        username = config.getString(CONFIG_KEY_USERNAME);
        password = config.getString(CONFIG_KEY_PASSWORD);
    }

    public ElasticsearchClusterConfig(HierarchicalConfiguration<?> config, String username, String password) {
        urls = config.getList(String.class, CONFIG_KEY_URLS);
        this.username = config.getString(CONFIG_KEY_USERNAME, username);
        this.password = config.getString(CONFIG_KEY_PASSWORD, password);
    }

    /**
     * Returns a client matching the current configuration of the cluster
     */
    public RestHighLevelClient buildClient() {
        HttpHost[] hosts = urls.stream().map(HttpHost::create).toArray(HttpHost[]::new);
        RestClientBuilder clientBuilder = RestClient.builder(hosts);
        if (StringUtils.isNoneEmpty(username, password)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
            clientBuilder
                .setHttpClientConfigCallback(builder -> builder.setDefaultCredentialsProvider(credentialsProvider));
        }
        return new RestHighLevelClient(clientBuilder);
    }

}
