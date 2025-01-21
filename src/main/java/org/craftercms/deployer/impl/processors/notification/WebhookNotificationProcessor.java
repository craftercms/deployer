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
package org.craftercms.deployer.impl.processors.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * {@link NotificationProcessor} implementation that sends a webhook notification with the result of the deployment.
 * The deployment is available to the template as an object with the key "deployment", and serialized as JSON with the key "deployentJson".
 * A {@link WebhookNotificationProcessor} instance can be configured with the following YAML properties (in addition to the common ones from {@link NotificationProcessor}):
 *
 * <ul>
 *     <li><strong>url:</strong> The URL to send the webhook notification to.</li>
 *     <li><strong>method:</strong> The HTTP method to use. Default is <code>post</code>.</li>
 *     <li><strong>contentType:</strong> The content type of the request body. Default is <code>application/json</code></li>
 * </ul>
 */
public class WebhookNotificationProcessor extends NotificationProcessor<NotificationProcessor.NotificationMessage> {
	private static final Logger logger = LoggerFactory.getLogger(WebhookNotificationProcessor.class);

	private static final String URL_CONFIG_KEY = "url";
	private static final String METHOD_CONFIG_KEY = "method";
	private static final String CONTENT_TYPE_CONFIG_KEY = "contentType";
	private static final String DEPLOYMENT_JSON_MODEL_KEY = "deploymentJson";

	private String defaultMethod;
	private String defaultContentType;

	private String method;
	private String url;
	private String contentType;
	private CloseableHttpClient httpClient;

	protected ObjectMapper objectMapper;

	@Override
	public void doInit(Configuration config) throws ConfigurationException, DeployerException {
		super.doInit(config);
		method = config.getString(METHOD_CONFIG_KEY, defaultMethod);
		contentType = config.getString(CONTENT_TYPE_CONFIG_KEY, defaultContentType);
		url = config.getString(URL_CONFIG_KEY);

		httpClient = HttpClients.createDefault();
	}

	@Override
	protected NotificationMessage doCreateMessage(Deployment deployment) {
		NotificationMessage message = new NotificationMessage();
		try {
			String deploymentJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(deployment);
			message.getModel().put(DEPLOYMENT_JSON_MODEL_KEY, deploymentJson);
		} catch (JsonProcessingException e) {
			logger.error("Failed to write deployment to JSON", e);
		}

		return message;
	}

	@Override
	protected void doNotify(NotificationProcessor.NotificationMessage message) throws DeployerException {
		logger.info("Sending webhook notification to {} with method {}", url, method);
		try {
			HttpUriRequest request = createRequest(message);
			try (CloseableHttpResponse response = httpClient.execute(request)) {
				logger.info("Webhook notification sent with status {}", response.getStatusLine());
			}
		} catch (IOException e) {
			logger.error("Error sending webhook notification", e);
			throw new DeployerException(e);
		}
	}

	/**
	 * Creates the HTTP request to send the notification.
	 *
	 * @param message The notification message.
	 */
	private HttpUriRequest createRequest(NotificationProcessor.NotificationMessage message) throws DeployerException {
		return RequestBuilder
			.create(method)
			.setUri(url)
			.setEntity(new StringEntity(message.getBody(), ContentType.getByMimeType(contentType)))
			.build();
	}

	public void setDefaultMethod(String defaultMethod) {
		this.defaultMethod = defaultMethod;
	}

	public void setDefaultContentType(String defaultContentType) {
		this.defaultContentType = defaultContentType;
	}

	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}
}
