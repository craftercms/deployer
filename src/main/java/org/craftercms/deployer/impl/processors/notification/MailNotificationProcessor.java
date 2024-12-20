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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.mail.Email;
import org.craftercms.commons.mail.EmailFactory;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.craftercms.commons.config.ConfigUtils.*;

/**
 * {@link NotificationProcessor} implementation that sends an email notification with the result of the deployment.
 * The output file is attached if it's available. A {@link MailNotificationProcessor} instance can be configured with
 * the following YAML properties (in addition to the common ones from {@link NotificationProcessor}):
 *
 * <ul>
 *     <li><strong>from:</strong> The value of the From field in the emails.</li>
 *     <li><strong>to:</strong> The value of the To field in the emails.</li>
 *     <li><strong>subject:</strong> The value of the Subject field in the emails.</li>
 *     <li><strong>html:</strong> Whether the emails are HTML.</li>
 * </ul>
 *
 * @author avasquez
 */
public class MailNotificationProcessor extends NotificationProcessor<MailNotificationProcessor.EmailMessage> {

    private static final Logger logger = LoggerFactory.getLogger(MailNotificationProcessor.class);

    public static final String FROM_CONFIG_KEY = "from";
    public static final String TO_CONFIG_KEY = "to";
    public static final String SUBJECT_CONFIG_KEY = "subject";
    public static final String HTML_CONFIG_KEY = "html";

    public static final String OUTPUT_ATTACHED_MODEL_KEY = "outputAttached";

    protected String defaultFrom;
    protected String defaultSubject;
    protected boolean defaultHtml;
    protected EmailFactory emailFactory;
    protected ObjectMapper objectMapper;

    // Config properties (populated on init)
    protected String from;
    protected String[] to;
    protected String subject;
    protected boolean html;

    /**
     * Sets the default value of the From field in the emails.
     */
    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    /**
     * Sets the default value of the Subject field in the emails.
     */
    public void setDefaultSubject(String defaultSubject) {
        this.defaultSubject = defaultSubject;
    }

    /**
     * Sets whether the emails are HTML by default.
     */
    public void setDefaultHtml(boolean defaultHtml) {
        this.defaultHtml = defaultHtml;
    }

    /**
     * Sets the {@link EmailFactory} used to generate the emails.
     */
    public void setEmailFactory(EmailFactory emailFactory) {
        this.emailFactory = emailFactory;
    }

    /**
     * Sets the {@link ObjectMapper} used to serialize the deployment result.
     */
    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doInit(Configuration config) throws ConfigurationException, DeployerException {
        super.doInit(config);
        from = getStringProperty(config, FROM_CONFIG_KEY, defaultFrom);
        to = getRequiredStringArrayProperty(config, TO_CONFIG_KEY);
        subject = getStringProperty(config, SUBJECT_CONFIG_KEY, defaultSubject);
        html = getBooleanProperty(config, HTML_CONFIG_KEY, defaultHtml);
    }

    @Override
    protected void doDestroy() {
        // Do nothing
    }

    @Override
    protected EmailMessage doCreateMessage(Deployment deployment) {
        EmailMessage message = new EmailMessage();
        File attachment = getAttachment(deployment);
        message.setAttachment(attachment);
        message.getModel().put(OUTPUT_ATTACHED_MODEL_KEY, attachment != null);
        return message;
    }

    @Override
    protected void doNotify(EmailMessage emailMessage) throws DeployerException {
        File attachment = emailMessage.getAttachment();
        try {
            Email email;

            if (attachment != null) {
                email = emailFactory.getEmail(from, to, null, null, subject, emailMessage.getBody(), html,
                        attachment);
            } else {
                email = emailFactory.getEmail(from, to, null, null, subject, emailMessage.getBody(), html);
            }

            email.send();
            logger.info("Deployment notification successfully sent to {}", Arrays.toString(to));
        } catch (Exception e) {
            throw new DeployerException("Error while sending email with deployment report", e);
        } finally {
            if (attachment != null) {
                attachment.delete();
            }
        }
    }

    /**
     * Returns a temporary file with the deployment as JSON.
     *
     * @param deployment The deployment object
     * @return The temporary file
     */
    private File getAttachment(Deployment deployment) {
        File tempFileDeployment = null;

        try {
            File attachment = File.createTempFile("deployment", ".json");
            objectMapper.writeValue(attachment, deployment);
            tempFileDeployment = attachment;
        } catch (IOException e) {
            logger.error("Could not write deployment as json", e);
        }
        return tempFileDeployment;
    }

    /**
     * {@link NotificationMessage} extension that includes a file attachment.
     */
    protected class EmailMessage extends NotificationMessage {
        private File attachment;

        public File getAttachment() {
            return attachment;
        }

        public void setAttachment(File attachment) {
            this.attachment = attachment;
        }
    }
}
