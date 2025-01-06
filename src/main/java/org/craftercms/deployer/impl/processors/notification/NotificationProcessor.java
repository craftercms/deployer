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

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.processors.AbstractPostDeploymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.craftercms.commons.config.ConfigUtils.getIntegerProperty;
import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Abstract processor that sends notifications based on the deployment status and the failed processors.
 * <p>
 * A {@link MailNotificationProcessor} instance can be configured with the following YAML properties:
 *
 * <ul>
 *     <li><strong>templateName:</strong> The name of the Freemarker template used for email creation.</li>
 *     <li><strong>serverName:</strong> The hostname of the email server.</li>
 *     <li><strong>status:</strong> The status condition that triggers the notification. Possible values are: SUCCESS (default), ON_ANY_STATUS, ON_ANY_FAILURE, ON_TOTAL_FAILURE.</li>
 *     <li><strong>failedProcessors:</strong> A regex pattern to match the failed processors name that trigger the notification.</li>
 *     <li><strong>mutePeriodMinutes:</strong> The number of minutes to wait before sending another notification for the same processor.</li>
 *     <li><strong>lastDateFilenameSuffix:</strong> The suffix to use when creating the last notification date file.</li>
 *     <li><strong>dateTimePattern:</strong> The date time pattern to use when specifying a date in the message.</li>
 * </ul>
 *
 * @param <T> Type of the message to send. Must extend {@link NotificationMessage}
 */
public abstract class NotificationProcessor<T extends NotificationProcessor.NotificationMessage> extends AbstractPostDeploymentProcessor {

    private static final Logger logger = LoggerFactory.getLogger(NotificationProcessor.class);

    public static final String DEFAULT_ENCODING = "UTF-8";

    public static final String PROCESSOR_MATCH_PATTERNS_CONFIG_KEY = "failedProcessors";
    public static final String MUTE_PERIOD_MINUTES_CONFIG_KEY = "mutePeriodMinutes";
    public static final String LAST_DATETIME_FILE_SUFFIX_CONFIG_KEY = "lastDateFilenameSuffix";
    public static final String STATUS_CONDITION_CONFIG_KEY = "status";
    public static final String SERVER_NAME_CONFIG_KEY = "serverName";
    public static final String TEMPLATE_NAME_CONFIG_KEY = "templateName";
    public static final String DATETIME_PATTERN_CONFIG_KEY = "dateTimePattern";

    public static final String SERVER_NAME_MODEL_KEY = "serverName";
    public static final String TARGET_ID_MODEL_KEY = "targetId";
    public static final String START_MODEL_KEY = "start";
    public static final String END_MODEL_KEY = "end";
    public static final String STATUS_MODEL_KEY = "status";
    public static final String DEPLOYMENT_MODEL_KEY = "deployment";

    private String defaultStatusCondition;
    private String defaultLastDateFilenameSuffix;
    private String defaultTemplateName;
    private String defaultDateTimePattern;
    private String lastNotificationDateDir;

    private freemarker.template.Configuration freeMarkerConfig;
    private String templatePrefix = "";
    private String templateSuffix = "";
    private String templateEncoding = DEFAULT_ENCODING;

    // Config properties (populated on init)
    protected String templateName;
    protected String serverName;
    protected StatusCondition statusCondition;
    protected Pattern failedProcessorsPattern;
    protected int mutePeriodMinutes;
    private String lastDateFilenameSuffix;
    protected DateTimeFormatter dateTimeFormatter;
    private String fullTemplateName;

    @Override
    public void doInit(Configuration config) throws ConfigurationException, DeployerException {
        statusCondition = StatusCondition.valueOf(getStringProperty(config, STATUS_CONDITION_CONFIG_KEY, defaultStatusCondition));

        String processorsMatchRegex = getStringProperty(config, PROCESSOR_MATCH_PATTERNS_CONFIG_KEY);
        if (processorsMatchRegex != null) {
            failedProcessorsPattern = Pattern.compile(processorsMatchRegex);
        }
        mutePeriodMinutes = getIntegerProperty(config, MUTE_PERIOD_MINUTES_CONFIG_KEY, 0);

        lastDateFilenameSuffix = getStringProperty(config, LAST_DATETIME_FILE_SUFFIX_CONFIG_KEY, defaultLastDateFilenameSuffix);

        templateName = getStringProperty(config, TEMPLATE_NAME_CONFIG_KEY, defaultTemplateName);
        fullTemplateName = templatePrefix + templateName + templateSuffix;

        serverName = getStringProperty(config, SERVER_NAME_CONFIG_KEY);
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
    protected void doDestroy() {
        // Do nothing
    }

    /**
     * Indicate if the deployment status (and processors' status) matches the configured status condition.
     */
    private boolean matchesStatusCondition(Deployment deployment) {
        Deployment.Status status = deployment.getStatus();

        return switch (statusCondition) {
            case SUCCESS -> status == Deployment.Status.SUCCESS;
            case ON_ANY_STATUS -> true;
            case ON_ANY_FAILURE -> hasExecutionsFailures(deployment);
            case ON_TOTAL_FAILURE -> status == Deployment.Status.FAILURE;
        };
    }

    @Override
    protected ChangeSet doPostProcess(Deployment deployment, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        Deployment.Status status = deployment.getStatus();
        if (!matchesStatusCondition(deployment)) {
            logger.info("Skipping notification because status '{}' does not match the condition '{}'",
                    status, statusCondition);
            return null;
        }

        String failedProcessor = getFailedProcessor(deployment);

        if (!matchFailedProcessor(failedProcessor)) {
            logger.info("Skipping notification because failed processors do not match the configured patterns");
            return null;
        }

        if (mutePeriodRunning(failedProcessor)) {
            logger.info("Skipping notification because the mute period has not expired");
            return null;
        }

        doNotify(createMessage(deployment));

        storeNotificationDate(failedProcessor);
        return null;
    }

    protected T createMessage(Deployment deployment) {
        T message = doCreateMessage(deployment);
        populateModel(deployment, message);
        return message;
    }

    /**
     * Creates the notification message to send.
     *
     * @param deployment the deployment object
     * @return the notification message
     */
    protected abstract T doCreateMessage(Deployment deployment);

    /**
     * Sends the notification message.
     *
     * @param message the notification message
     * @throws DeployerException if an error occurs while sending the notification
     */
    protected abstract void doNotify(T message) throws DeployerException;

    /**
     * Returns the last notification date file for the given processor.
     */
    private File getLastDateFile(String processor) {
        String filename = "%s-%s-%s%s".formatted(targetId, name, processor, lastDateFilenameSuffix);
        return new File(lastNotificationDateDir, filename);
    }

    /**
     * Stores the current date as the last notification date for the given processor.
     */
    private void storeNotificationDate(String processor) {
        if (processor == null) {
            // No mute period applies to successful deployments
            return;
        }
        if (mutePeriodMinutes <= 0) {
            // No need to store the timestamp if there is no mute period
            return;
        }
        try {
            FileUtils.write(getLastDateFile(processor), String.valueOf(System.currentTimeMillis()), DEFAULT_ENCODING, false);
        } catch (IOException e) {
            logger.warn("Could not store last notification date", e);
        }
    }

    /**
     * Indicates if the mute period for the given processor is still running.
     * If the mute period is running, the notification will be skipped.
     */
    private boolean mutePeriodRunning(String processor) {
        if (processor == null) {
            logger.info("Mute period does not apply for successful deployments, sending notification");
            return false;
        }
        File lastDateFile = getLastDateFile(processor);
        if (mutePeriodMinutes <= 0) {
            logger.info("Mute period is 0 or negative, sending notification");
            return false;
        }
        if (!lastDateFile.exists()) {
            logger.info("No last notification date file found, sending notification");
            return false;
        }
        try {
            String lastDate = FileUtils.readFileToString(lastDateFile, "UTF-8").trim();
            long lastEmailMillis = Long.parseLong(lastDate);
            long mutePeriodMillis = mutePeriodMinutes * 60 * 1000L;
            if (System.currentTimeMillis() - lastEmailMillis < mutePeriodMillis) {
                logger.info("Mute period has not expired, skipping notification");
                return true;
            }
        } catch (NumberFormatException e) {
            logger.error("Could not parse last notification date from file '{}'", lastDateFile, e);
        } catch (IOException e) {
            logger.error("Could not read last notification date from file '{}'", lastDateFile, e);
        }

        return false;
    }

    /**
     * Populates the model with the deployment information.
     *
     * @param deployment the deployment object
     * @param message    the notification message
     */
    protected void populateModel(Deployment deployment, T message) {
        Map<String, Object> templateModel = message.getModel();
        templateModel.put(SERVER_NAME_MODEL_KEY, serverName);
        templateModel.put(TARGET_ID_MODEL_KEY, deployment.getTarget().getId());
        templateModel.put(START_MODEL_KEY, deployment.getStart().format(dateTimeFormatter));
        templateModel.put(END_MODEL_KEY, deployment.getEnd().format(dateTimeFormatter));
        templateModel.put(STATUS_MODEL_KEY, deployment.getStatus());
        templateModel.put(DEPLOYMENT_MODEL_KEY, deployment);
    }

    /**
     * Processes the notification template with the given model.
     *
     * @param templateModel the model to use for processing the template
     * @return the template output
     * @throws DeployerException if an error occurs while loading or processing the template
     */
    protected String processTemplate(Map<String, Object> templateModel) throws DeployerException {
        logger.debug("Processing notification template '{}'", templateName);

        try {
            Template template = freeMarkerConfig.getTemplate(fullTemplateName, templateEncoding);
            StringWriter out = new StringWriter();

            template.process(templateModel, out);

            return out.toString();
        } catch (IOException | TemplateException e) {
            throw new DeployerException(e);
        }
    }

    /**
     * Indicates if any failed deployment processor matches the configured patterns.
     */
    private boolean matchFailedProcessor(final String failedProcessor) {
        return failedProcessorsPattern == null ||
                failedProcessorsPattern.matcher(failedProcessor).matches();
    }

    /**
     * Returns the name of the first failed processor in the deployment pipeline.
     */
    private String getFailedProcessor(Deployment deployment) {
        return deployment.getProcessorExecutions().stream()
                .filter(ex -> ex.getStatus() == Deployment.Status.FAILURE)
                .map(ProcessorExecution::getProcessorName)
                .findFirst().orElse(null);
    }

    /**
     * Indicates if any of the processor executions in the deployment has failed.
     */
    private boolean hasExecutionsFailures(Deployment deployment) {
        for (ProcessorExecution execution : deployment.getProcessorExecutions()) {
            if (execution.getStatus() == Deployment.Status.FAILURE) {
                return true;
            }
        }
        return false;
    }

    public void setDefaultDateTimePattern(String defaultDateTimePattern) {
        this.defaultDateTimePattern = defaultDateTimePattern;
    }

    public void setDefaultLastDateFilenameSuffix(String defaultLastDateFilenameSuffix) {
        this.defaultLastDateFilenameSuffix = defaultLastDateFilenameSuffix;
    }

    public void setDefaultStatusCondition(String defaultStatusCondition) {
        this.defaultStatusCondition = defaultStatusCondition;
    }

    public void setDefaultTemplateName(String defaultTemplateName) {
        this.defaultTemplateName = defaultTemplateName;
    }

    public void setLastNotificationDateDir(String lastNotificationDateDir) {
        this.lastNotificationDateDir = lastNotificationDateDir;
    }

    public void setFreeMarkerConfig(freemarker.template.Configuration freeMarkerConfig) {
        this.freeMarkerConfig = freeMarkerConfig;
    }

    public void setTemplatePrefix(String templatePrefix) {
        this.templatePrefix = templatePrefix;
    }

    public void setTemplateSuffix(String templateSuffix) {
        this.templateSuffix = templateSuffix;
    }

    public void setTemplateEncoding(String templateEncoding) {
        this.templateEncoding = templateEncoding;
    }

    /**
     * Status conditions used to control whe the notifications should be sent.
     *
     * @author joseross
     */
    public enum StatusCondition {
        /**
         * Notifications will be sent for successful deployments only
         */
        SUCCESS,
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

    /**
     * Base class for notification messages.
     * {@link NotificationProcessor} implementations may extend this class to provide custom messages.
     */
    public class NotificationMessage {
        private final Map<String, Object> model = new HashMap<>();

        public Map<String, Object> getModel() {
            return model;
        }

        public String getBody() throws DeployerException {
            return processTemplate(getModel());
        }
    }
}
