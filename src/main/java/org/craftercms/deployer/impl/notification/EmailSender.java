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

import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.mail.Email;
import org.craftercms.commons.mail.EmailFactory;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.notification.NotificationSender;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.craftercms.commons.config.ConfigUtils.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link NotificationSender} implementation that sends an email notification.
 * The output file is attached if it's available. An {@link EmailSender} instance can be configured with
 * the following YAML properties:
 *
 * <ul>
 *     <li><strong>from:</strong> The value of the From field in the emails.</li>
 *     <li><strong>to:</strong> The value of the To field in the emails.</li>
 *     <li><strong>subject:</strong> The value of the Subject field in the emails.</li>
 *     <li><strong>html:</strong> Whether the emails are HTML.</li>
 * </ul>
 */
public class EmailSender extends NotificationSender<EmailSender.EmailMessage> {
	private static final Logger logger = getLogger(EmailSender.class);

	public static final String FROM_CONFIG_KEY = "from";
	public static final String TO_CONFIG_KEY = "to";
	public static final String SUBJECT_CONFIG_KEY = "subject";
	public static final String HTML_CONFIG_KEY = "html";

	public static final String OUTPUT_ATTACHED_MODEL_KEY = "outputAttached";

	protected String defaultFrom;
	protected String defaultSubject;
	protected boolean defaultHtml;
	protected EmailFactory emailFactory;

	// Config properties (populated on init)
	protected String from;
	protected String[] to;
	protected String subject;
	protected boolean html;

	@Override
	public void init(Configuration config) throws ConfigurationException, DeployerException {
		super.init(config);
		from = getStringProperty(config, FROM_CONFIG_KEY, defaultFrom);
		to = getRequiredStringArrayProperty(config, TO_CONFIG_KEY);
		subject = getStringProperty(config, SUBJECT_CONFIG_KEY, defaultSubject);
		html = getBooleanProperty(config, HTML_CONFIG_KEY, defaultHtml);
	}

	@Override
	protected void doNotify(EmailMessage message) throws DeployerException {
		File attachment = message.getAttachment();
		try {
			Email email;

			if (attachment != null) {
				email = emailFactory.getEmail(from, to, null, null, subject, message.getBody(), html,
						attachment);
			} else {
				email = emailFactory.getEmail(from, to, null, null, subject, message.getBody(), html);
			}

			email.send();
			logger.info("Email notification successfully sent to {}", Arrays.toString(to));
		} catch (Exception e) {
			throw new DeployerException("Error while sending email notification", e);
		} finally {
			deleteQuietly(attachment);
		}
	}

	@Override
	protected EmailMessage doCreateMessage(String templateName, Object payload) {
		File attachment = getAttachment(payload);
		EmailMessage message = new EmailMessage(templateName, attachment);
		message.getModel().put(OUTPUT_ATTACHED_MODEL_KEY, attachment != null);
		return message;
	}

	/**
	 * Returns a temporary file with the notification payload as JSON.
	 *
	 * @param payload The payload object to be serialized
	 * @return The temporary file
	 */
	private File getAttachment(Object payload) {
		File tempFilePayload = null;

		try {
			File attachment = File.createTempFile("payload", ".json");
			objectMapper.writeValue(attachment, payload);
			tempFilePayload = attachment;
		} catch (IOException e) {
			logger.error("Failed to write payload to JSON", e);
		}
		return tempFilePayload;
	}

	@SuppressWarnings("unused")
	public void setDefaultFrom(String defaultFrom) {
		this.defaultFrom = defaultFrom;
	}

	@SuppressWarnings("unused")
	public void setDefaultHtml(boolean defaultHtml) {
		this.defaultHtml = defaultHtml;
	}

	@SuppressWarnings("unused")
	public void setDefaultSubject(String defaultSubject) {
		this.defaultSubject = defaultSubject;
	}

	@SuppressWarnings("unused")
	public void setEmailFactory(EmailFactory emailFactory) {
		this.emailFactory = emailFactory;
	}

	/**
	 * {@link NotificationMessage} extension that includes a file attachment.
	 */
	protected class EmailMessage extends NotificationSender<?>.NotificationMessage {
		private final File attachment;

		public EmailMessage(String templateName, File attachment) {
			super(templateName);
			this.attachment = attachment;
		}

		public File getAttachment() {
			return attachment;
		}
	}
}
