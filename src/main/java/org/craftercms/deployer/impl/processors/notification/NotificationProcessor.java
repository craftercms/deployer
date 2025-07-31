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
package org.craftercms.deployer.impl.processors.notification;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.notification.NotificationSender;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.impl.ProcessorStateStore;
import org.craftercms.deployer.impl.processors.AbstractPostDeploymentProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.craftercms.commons.config.ConfigUtils.getIntegerProperty;
import static org.craftercms.commons.config.ConfigUtils.getStringProperty;

/**
 * Processor that sends notifications based on the deployment status and the failed processors.
 * <p>
 * A {@link NotificationProcessor} instance can be configured with the following YAML properties:
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
 */
public class NotificationProcessor extends AbstractPostDeploymentProcessor {

	private static final Logger logger = LoggerFactory.getLogger(NotificationProcessor.class);

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
	private ProcessorStateStore processorStateStore;
	protected NotificationSender<?> notificationSender;

	// Config properties (populated on init)
	protected String templateName;
	protected String serverName;
	protected StatusCondition statusCondition;
	protected Pattern failedProcessorsPattern;
	protected int mutePeriodMinutes;
	private String lastDateFilenameSuffix;
	protected DateTimeFormatter dateTimeFormatter;

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

		notificationSender.init(config);
	}

	@Override
	protected void doDestroy() {
		try {
			notificationSender.close();
		} catch (Exception e) {
			logger.error("Error while closing notification sender", e);
		}
	}

	/**
	 * Indicate if the deployment status (and processors' status) matches the configured status condition.
	 */
	private boolean matchesStatusCondition(Deployment deployment) {
		Deployment.Status status = deployment.getStatus();

		return switch (statusCondition) {
			case SUCCESS -> status == Deployment.Status.SUCCESS;
			case ON_ANY_STATUS -> true;
			case ON_ANY_FAILURE -> status == Deployment.Status.FAILURE || hasExecutionsFailures(deployment);
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

		try {
			notificationSender.sendMessage(templateName, deployment, getModel(deployment));
		} catch (Exception e) {
			throw new DeployerException("Failed to send notification for deployment", e);
		}

		storeNotificationDate(failedProcessor);
		return null;
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
			processorStateStore.store(targetId, name, getStateFileSuffix(processor), String.valueOf(System.currentTimeMillis()));
		} catch (IOException e) {
			logger.warn("Could not store last notification date", e);
		}
	}

	private String getStateFileSuffix(String processor) {
		return "%s%s".formatted(processor, lastDateFilenameSuffix);
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
		if (mutePeriodMinutes <= 0) {
			logger.info("Mute period is 0 or negative, sending notification");
			return false;
		}

		try {
			String lastNotificationTimestamp = processorStateStore.load(targetId, name, getStateFileSuffix(processor));
			if (lastNotificationTimestamp == null) {
				logger.info("No last notification date found, sending notification");
				return false;
			}
			long lastNotificationMillis = Long.parseLong(lastNotificationTimestamp);
			long mutePeriodMillis = mutePeriodMinutes * 60 * 1000L;
			if (System.currentTimeMillis() - lastNotificationMillis < mutePeriodMillis) {
				logger.info("Mute period has not expired, skipping notification");
				return true;
			}
		} catch (NumberFormatException e) {
			logger.error("Could not parse last notification date", e);

		} catch (IOException e) {
			logger.error("Could not read last notification date from store", e);
		}

		return false;
	}

	/**
	 * Get the model with the deployment information.
	 *
	 * @param deployment the deployment object
	 */
	protected Map<String, Object> getModel(Deployment deployment) {
		Map<String, Object> templateModel = new HashMap<>();
		templateModel.put(SERVER_NAME_MODEL_KEY, serverName);
		templateModel.put(TARGET_ID_MODEL_KEY, deployment.getTarget().getId());
		templateModel.put(START_MODEL_KEY, deployment.getStart().format(dateTimeFormatter));
		templateModel.put(END_MODEL_KEY, deployment.getEnd().format(dateTimeFormatter));
		templateModel.put(STATUS_MODEL_KEY, deployment.getStatus());
		templateModel.put(DEPLOYMENT_MODEL_KEY, deployment);
		return templateModel;
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

	@SuppressWarnings("unused")
	public void setDefaultDateTimePattern(String defaultDateTimePattern) {
		this.defaultDateTimePattern = defaultDateTimePattern;
	}

	@SuppressWarnings("unused")
	public void setDefaultLastDateFilenameSuffix(String defaultLastDateFilenameSuffix) {
		this.defaultLastDateFilenameSuffix = defaultLastDateFilenameSuffix;
	}

	@SuppressWarnings("unused")
	public void setDefaultStatusCondition(String defaultStatusCondition) {
		this.defaultStatusCondition = defaultStatusCondition;
	}

	@SuppressWarnings("unused")
	public void setDefaultTemplateName(String defaultTemplateName) {
		this.defaultTemplateName = defaultTemplateName;
	}

	@SuppressWarnings("unused")
	public void setProcessorStateStore(ProcessorStateStore processorStateStore) {
		this.processorStateStore = processorStateStore;
	}

	@SuppressWarnings("unused")
	public void setNotificationSender(NotificationSender<?> notificationSender) {
		this.notificationSender = notificationSender;
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
}
