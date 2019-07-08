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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.mail.Email;
import org.craftercms.commons.mail.EmailFactory;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.craftercms.deployer.utils.ConfigUtils.*;

/**
 * Post processor that sends an email notification with the result of a deployment, whenever a deployment fails or files where processed.
 * The output file is attached if it's available. A processor instance can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>templateName:</strong> The name of the Freemarker template used for email creation.</li>
 *     <li><strong>from:</strong> The value of the From field in the emails.</li>
 *     <li><strong>to:</strong> The value of the To field in the emails.</li>
 *     <li><strong>subject:</strong> The value of the Subject field in the emails.</li>
 *     <li><strong>html:</strong> Whether the emails are HTML.</li>
 *     <li><strong>serverName:</strong> The hostname of the email server.</li>
 *     <li><strong>dateTimePattern:</strong> The date time pattern to use when specifying a date in the email.</li>
 * </ul>
 *
 * @author avasquez
 */
public class MailNotificationProcessor extends AbstractPostDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(MailNotificationProcessor.class);

    /**
     * Status conditions used to control whe the notifications should be sent.
     * @author joseross
     */
    public enum StatusCondition {
        /**
         * Notifications will be sent for all deployments.
         */
        ON_ANY_STATUS,

        /**
         * Notifications will be sent for deployments in which at least one processor has failed.
         */
        ON_ANY_FAILURE,

        /**
         * Notifications will be sent for deployments in which the general status indicates failure.
         */
        ON_TOTAL_FAILURE
    }

    public static final String TEMPLATE_NAME_CONFIG_KEY = "templateName";
    public static final String FROM_CONFIG_KEY = "from";
    public static final String TO_CONFIG_KEY = "to";
    public static final String SUBJECT_CONFIG_KEY = "subject";
    public static final String HTML_CONFIG_KEY = "html";
    public static final String SERVER_NAME_CONFIG_KEY = "serverName";
    public static final String STATUS_CONDITION_KEY = "status";
    public static final String DATETIME_PATTERN_CONFIG_KEY = "dateTimePattern";

    public static final String SERVER_NAME_MODEL_KEY = "serverName";
    public static final String TARGET_ID_MODEL_KEY = "targetId";
    public static final String START_MODEL_KEY = "start";
    public static final String END_MODEL_KEY = "end";
    public static final String STATUS_MODEL_KEY = "status";
    public static final String OUTPUT_ATTACHED_MODEL_KEY = "outputAttached";

    protected String defaultTemplateName;
    protected String defaultFrom;
    protected String defaultSubject;
    protected boolean defaultHtml;
    protected String defaultStatusCondition;
    protected String defaultDateTimePattern;
    protected EmailFactory emailFactory;
    protected ObjectMapper objectMapper;

    // Config properties (populated on init)

    protected String templateName;
    protected String from;
    protected String[] to;
    protected String subject;
    protected boolean html;
    protected String serverName;
    protected StatusCondition statusCondition;
    protected DateTimeFormatter dateTimeFormatter;

    /**
     * Sets the default name of the Freemarker template used for email creation.
     */
    @Required
    public void setDefaultTemplateName(String defaultTemplateName) {
        this.defaultTemplateName = defaultTemplateName;
    }

    /**
     * Sets the default value of the From field in the emails.
     */
    @Required
    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    /**
     * Sets the default value of the Subject field in the emails.
     */
    @Required
    public void setDefaultSubject(String defaultSubject) {
        this.defaultSubject = defaultSubject;
    }

    /**
     * Sets whether the emails are HTML by default.
     */
    @Required
    public void setDefaultHtml(boolean defaultHtml) {
        this.defaultHtml = defaultHtml;
    }

    /** Sets the default condition to send emails.
     */
    @Required
    public void setDefaultStatusCondition(final String defaultStatusCondition) {
        this.defaultStatusCondition = defaultStatusCondition;
    }

    /**
     * Sets the default date time pattern to use when specifying a date in the email.
     */
    @Required
    public void setDefaultDateTimePattern(String defaultDateTimePattern) {
        this.defaultDateTimePattern = defaultDateTimePattern;
    }

    /**
     * Sets the {@link EmailFactory} used to generate the emails.
     */
    @Required
    public void setEmailFactory(EmailFactory emailFactory) {
        this.emailFactory = emailFactory;
    }

    /**
     * Sets the {@link ObjectMapper} used to serialize the deployment result.
     */
    @Required
    public void setObjectMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void doInit(Configuration config) throws ConfigurationException, DeployerException {
        templateName = getStringProperty(config, TEMPLATE_NAME_CONFIG_KEY, defaultTemplateName);
        from = getStringProperty(config, FROM_CONFIG_KEY, defaultFrom);
        to = getRequiredStringArrayProperty(config, TO_CONFIG_KEY);
        subject = getStringProperty(config, SUBJECT_CONFIG_KEY, defaultSubject);
        html = getBooleanProperty(config, HTML_CONFIG_KEY, defaultHtml);
        serverName = getStringProperty(config, SERVER_NAME_CONFIG_KEY);
        statusCondition = StatusCondition.valueOf(getStringProperty(config, STATUS_CONDITION_KEY, defaultStatusCondition));

        if (StringUtils.isEmpty(serverName)) {
            try {
                serverName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                throw new DeployerException("Unable to retrieve localhost address", e);
            }
        }

        String dateTimePattern = getStringProperty(config, DATETIME_PATTERN_CONFIG_KEY, defaultDateTimePattern);
        dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
    }

    @Override
    protected void doDestroy() throws DeployerException {
        // Do nothing
    }

    @Override
    protected ChangeSet doPostProcess(Deployment deployment, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        Deployment.Status status = deployment.getStatus();
        if ((statusCondition == StatusCondition.ON_TOTAL_FAILURE && status != Deployment.Status.FAILURE) ||
            (statusCondition == StatusCondition.ON_ANY_FAILURE && !hasExecutionsFailures(deployment))) {
                logger.info("Skipping notification because status '{}' does not match the condition '{}'",
                            status, statusCondition);
                return null;
        }

        Map<String, Object> templateModel = new HashMap<>();
        templateModel.put(SERVER_NAME_MODEL_KEY, serverName);
        templateModel.put(TARGET_ID_MODEL_KEY, deployment.getTarget().getId());
        templateModel.put(START_MODEL_KEY, deployment.getStart().format(dateTimeFormatter));
        templateModel.put(END_MODEL_KEY, deployment.getEnd().format(dateTimeFormatter));
        templateModel.put(STATUS_MODEL_KEY, deployment.getStatus());

        List<File> attachments = new ArrayList<>();
        File tempFile = null;

        try {
            tempFile = File.createTempFile("deployment", ".json");
            objectMapper.writeValue(tempFile, deployment);
            attachments.add(tempFile);
        } catch (IOException e) {
            logger.error("Could not write deployment as json", e);
        }

        File attachment = (File) deployment.getParam(FileOutputProcessor.OUTPUT_FILE_PARAM_NAME);
        if(attachment != null) {
            attachments.add(attachment);
        }

        templateModel.put(OUTPUT_ATTACHED_MODEL_KEY, !attachments.isEmpty());

        try {
            Email email;

            if (!attachments.isEmpty()) {
                email = emailFactory.getEmail(from, to, null, null, subject, templateName, templateModel, html,
                    attachments.toArray(new File[]{}));
            } else {
                email = emailFactory.getEmail(from, to, null, null, subject, templateName, templateModel, html);
            }

            email.send();

            logger.info("Deployment notification successfully sent to {}", Arrays.toString(to));
        } catch (Exception e) {
            throw new DeployerException("Error while sending email with deployment report", e);
        } finally {
            if(tempFile != null) {
                tempFile.delete();
            }
        }

        return null;
    }

    protected boolean hasExecutionsFailures(Deployment deployment) {
        for (ProcessorExecution execution : deployment.getProcessorExecutions()) {
            if (execution.getStatus() == Deployment.Status.FAILURE) {
                return true;
            }
        }
        return false;
    }

}
