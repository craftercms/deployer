/*
 * Copyright (C) 2007-2025 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.impl.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.annotation.PreDestroy;
import org.apache.commons.configuration2.Configuration;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.notification.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * {@link NotificationSender} implementation that sends a webhook notification.
 * The payload is available to the template as an object with the key "payload", and serialized as JSON with the key "payloadJson".
 * A {@link WebhookSender} instance can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>url:</strong> The URL to send the webhook notification to.</li>
 *     <li><strong>method:</strong> The HTTP method to use. Default is <code>post</code>.</li>
 *     <li><strong>contentType:</strong> The content type of the request body. Default is <code>application/json</code></li>
 * </ul>
 */
public class WebhookSender extends NotificationSender<NotificationSender<?>.NotificationMessage> {
	private static final Logger logger = LoggerFactory.getLogger(WebhookSender.class);

	private static final Set<String> VALID_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

	private static final String URL_CONFIG_KEY = "url";
	private static final String METHOD_CONFIG_KEY = "method";
	private static final String CONTENT_TYPE_CONFIG_KEY = "contentType";
	private static final String PAYLOAD_JSON_MODEL_KEY = "payloadJson";

	private String defaultMethod;
	private String defaultContentType;
	private int timeout;

	private String method;
	private String url;
	private String contentType;
	private CloseableHttpClient httpClient;
	private RequestConfig requestConfig;

	@Override
	public void init(Configuration config) throws ConfigurationException, DeployerException {
		super.init(config);
		method = config.getString(METHOD_CONFIG_KEY, defaultMethod);
		if (isEmpty(method)) {
			logger.error("HTTP method is required for WebhookSender");
			throw new ConfigurationException("HTTP method is required for WebhookSender");
		}

		method = method.toUpperCase();
		if (!VALID_METHODS.contains(method)) {
			logger.error("Invalid HTTP method '{}' specified for WebhookSender. Valid methods are: {}", method, VALID_METHODS);
			throw new ConfigurationException("Invalid HTTP method specified for WebhookSender: " + method);
		}
		contentType = config.getString(CONTENT_TYPE_CONFIG_KEY, defaultContentType);
		url = config.getString(URL_CONFIG_KEY);

		httpClient = HttpClients.createDefault();
		requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeout)
				.setSocketTimeout(timeout)
				.build();

	}

	@PreDestroy
	public void destroy() throws IOException {
		if (httpClient != null) {
			httpClient.close();
		}
	}

	@Override
	protected NotificationMessage doCreateMessage(String templateName, Object payload) {
		NotificationMessage message = new NotificationMessage(templateName);
		try {
			String payloadJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
			message.getModel().put(PAYLOAD_JSON_MODEL_KEY, payloadJson);
		} catch (JsonProcessingException e) {
			logger.error("Failed to write event payload to JSON", e);
		}

		return message;
	}

	@Override
	protected void doNotify(NotificationSender<?>.NotificationMessage message) throws DeployerException {
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
	private HttpUriRequest createRequest(NotificationSender<?>.NotificationMessage message) throws DeployerException {
		return RequestBuilder
				.create(method)
				.setUri(url)
				.setConfig(requestConfig)
				.setEntity(new StringEntity(message.getBody(), ContentType.getByMimeType(contentType)))
				.build();
	}

	@SuppressWarnings("unused")
	public void setDefaultMethod(String defaultMethod) {
		this.defaultMethod = defaultMethod;
	}

	@SuppressWarnings("unused")
	public void setDefaultContentType(String defaultContentType) {
		this.defaultContentType = defaultContentType;
	}

	@SuppressWarnings("unused")
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
}
