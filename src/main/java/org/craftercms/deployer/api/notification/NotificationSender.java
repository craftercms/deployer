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

package org.craftercms.deployer.api.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.DeploymentConstants;
import org.craftercms.deployer.utils.beans.InitializableByConfigBean;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.apache.commons.lang3.time.DurationFormatUtils.formatDuration;
import static org.craftercms.commons.config.ConfigUtils.DEFAULT_ENCODING;
import static org.craftercms.commons.config.ConfigUtils.getStringProperty;
import static org.craftercms.deployer.impl.DeploymentConstants.DATETIME_PATTERN_CONFIG_KEY;
import static org.craftercms.deployer.impl.DeploymentConstants.DURATION_PATTERN_CONFIG_KEY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Base class for notification senders.
 *
 * @param <M> the type of the notification message that will be created and sent
 */
public abstract class NotificationSender<M extends NotificationSender<?>.NotificationMessage> implements InitializableByConfigBean {

	private static final Logger logger = getLogger(NotificationSender.class);

	public static final String SERVER_NAME_MODEL_KEY = "serverName";
	public static final String PAYLOAD_MODEL_KEY = "payload";
	public static final String EVENT_MODEL_KEY = "event";
	public static final String TARGET_MODEL_KEY = "target";
	public static final String DATETIME_FORMATTER_KEY = "dateTimeFormatter";
	public static final String DURATION_FORMATTER_KEY = "durationFormatter";

	private String defaultDateTimePattern;
	private String defaultDurationPattern;

	private freemarker.template.Configuration freeMarkerConfig;
	private String templateEncoding = DEFAULT_ENCODING;
	private String templatePrefix = "";
	private String templateSuffix = "";
	protected ObjectMapper objectMapper;

	protected String serverName;
	protected DateTimeFormatter dateTimeFormatter;
	protected Function<Long, String> durationFormatter;

	@Override
	public void init(Configuration config) throws ConfigurationException, DeployerException {
		serverName = getStringProperty(config, DeploymentConstants.SERVER_NAME_CONFIG_KEY);
		if (StringUtils.isEmpty(serverName)) {
			try {
				serverName = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				throw new DeployerException("Unable to retrieve localhost address", e);
			}
		}
		String dateTimePattern = getStringProperty(config, DATETIME_PATTERN_CONFIG_KEY, defaultDateTimePattern);
		dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);

		String durationPattern = getStringProperty(config, DURATION_PATTERN_CONFIG_KEY, defaultDurationPattern);
		durationFormatter = (durationMillis) -> formatDuration(durationMillis, durationPattern);
	}

	/**
	 * Sends a notification message
	 *
	 * @param templateName the name of the template to use for the notification
	 * @param payload      the payload to include in the notification
	 * @param model        additional model data to include in the notification
	 * @throws DeployerException if an error occurs while sending the notification
	 */
	public void sendMessage(String templateName, Object payload, Map<String, Object> model) throws DeployerException {
		doNotify(createMessage(templateName, payload, model));
	}

	/**
	 * Creates a notification message using the specified template name, target, and payload.
	 *
	 * @param templateName the name of the template to use for the notification
	 * @param payload      the payload to include in the notification
	 * @param model        additional model data to include in the notification
	 * @return the notification message
	 */
	protected M createMessage(String templateName, Object payload, Map<String, Object> model) {
		M message = doCreateMessage(templateName, payload);
		Map<String, Object> templateModel = message.getModel();
		templateModel.put(SERVER_NAME_MODEL_KEY, serverName);
		templateModel.put(DATETIME_FORMATTER_KEY, dateTimeFormatter);
		templateModel.put(DURATION_FORMATTER_KEY, durationFormatter);
		templateModel.put(PAYLOAD_MODEL_KEY, payload);
		templateModel.putAll(model);
		return message;
	}

	/**
	 * Sends the notification message.
	 *
	 * @param message the notification message
	 * @throws DeployerException if an error occurs while sending the notification
	 */
	protected abstract void doNotify(M message) throws DeployerException;

	/**
	 * Creates the notification message to send.
	 *
	 * @param templateName the name of the template to use for the notification
	 * @param payload      the payload to include in the notification
	 * @return the notification message
	 */
	protected abstract M doCreateMessage(String templateName, Object payload);

	@SuppressWarnings("unused")
	public void setDefaultDateTimePattern(String defaultDateTimePattern) {
		this.defaultDateTimePattern = defaultDateTimePattern;
	}

	@SuppressWarnings("unused")
	public void setDefaultDurationPattern(String defaultDurationPattern) {
		this.defaultDurationPattern = defaultDurationPattern;
	}

	@SuppressWarnings("unused")
	public void setTemplatePrefix(String templatePrefix) {
		this.templatePrefix = templatePrefix;
	}

	@SuppressWarnings("unused")
	public void setTemplateSuffix(String templateSuffix) {
		this.templateSuffix = templateSuffix;
	}

	@SuppressWarnings("unused")
	public void setFreeMarkerConfig(freemarker.template.Configuration freeMarkerConfig) {
		this.freeMarkerConfig = freeMarkerConfig;
	}

	@SuppressWarnings("unused")
	public void setObjectMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@SuppressWarnings("unused")
	public void setTemplateEncoding(String templateEncoding) {
		this.templateEncoding = templateEncoding;
	}

	/**
	 * Processes the notification template with the given model.
	 *
	 * @param templateModel the model to use for processing the template
	 * @return the template output
	 * @throws DeployerException if an error occurs while loading or processing the template
	 */
	protected String processTemplate(String templateName, Map<String, Object> templateModel) throws DeployerException {
		logger.debug("Processing notification template '{}'", templateName);
		if (StringUtils.isEmpty(templateName)) {
			throw new DeployerException("Template name cannot be empty");
		}
		try {
			String fullTemplateName = templatePrefix + templateName + templateSuffix;
			Template template = freeMarkerConfig.getTemplate(fullTemplateName, templateEncoding);
			StringWriter out = new StringWriter();

			template.process(templateModel, out);

			return out.toString();
		} catch (IOException | TemplateException e) {
			throw new DeployerException(e);
		}
	}

	/**
	 * Base class for notification messages.
	 * {@link NotificationSender} implementations may extend this class to provide custom messages.
	 */
	public class NotificationMessage {
		private final Map<String, Object> model = new HashMap<>();
		private final String templateName;

		public NotificationMessage(String templateName) {
			this.templateName = templateName;
		}

		public Map<String, Object> getModel() {
			return model;
		}

		public String getBody() throws DeployerException {
			return processTemplate(templateName, getModel());
		}
	}
}
