/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.impl.processors;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.craftercms.deployer.utils.ConfigUtils.getRequiredStringProperty;

/**
 * Processor that does an HTTP method call. A processor instance can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>url:</strong> The URL to call</li>
 *     <li><strong>method:</strong> The HTTP method</li>
 * </ul>
 *
 * @author avasquez
 */
public class HttpMethodCallProcessor extends AbstractMainDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(HttpMethodCallProcessor.class);

    protected static final String URL_CONFIG_KEY = "url";
    protected static final String METHOD_CONFIG_KEY = "method";

    // Config properties (populated on init)

    protected String url;
    protected String method;
    protected CloseableHttpClient httpClient;

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        url = getRequiredStringProperty(config, URL_CONFIG_KEY);
        method = getRequiredStringProperty(config, METHOD_CONFIG_KEY);
        httpClient = HttpClients.createDefault();
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution,
                                      ChangeSet filteredChangeSet, ChangeSet originalChangeSet) throws DeployerException {
        HttpUriRequest request = createRequest();

        logger.info("Executing request {}...", request);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int status = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String body = entity != null? EntityUtils.toString(entity) : null;

            if (StringUtils.isEmpty(body)) {
                body = "empty";
            }

            if (status >= 200 && status < 300) {
                logger.info("Successful response for request {}: status = {}, body = {}", request, status, body);

                execution.setStatusDetails("Successful response for request " + request + ": status = " + status);
            } else {
                logger.error("Error response for request {}: status = {}, body = {}", request, status, body);

                execution.setStatusDetails("Error response for request " + request + ": status = " + status);
                execution.endExecution(Deployment.Status.FAILURE);
            }
        } catch (IOException e) {
            throw new DeployerException("IO error on HTTP request " + request, e);
        }

        return null;
    }

    @Override
    protected boolean failDeploymentOnProcessorFailure() {
        return false;
    }

    protected HttpUriRequest createRequest() throws DeployerException {
        if (method.equalsIgnoreCase("get")) {
            return new HttpGet(url);
        } else if (method.equalsIgnoreCase("post")) {
            return new HttpPost(url);
        } else if (method.equalsIgnoreCase("put")) {
            return new HttpPut(url);
        } else if (method.equalsIgnoreCase("delete")) {
            return new HttpDelete(url);
        } else if (method.equalsIgnoreCase("head")) {
            return new HttpHead(url);
        } else if (method.equalsIgnoreCase("options")) {
            return new HttpOptions(url);
        } else if (method.equalsIgnoreCase("trace")) {
            return new HttpTrace(url);
        } else {
            throw new DeployerException("HTTP method '" + method + " not recognized");
        }
    }

}
