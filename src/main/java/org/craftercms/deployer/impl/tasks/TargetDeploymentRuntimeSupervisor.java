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
package org.craftercms.deployer.impl.tasks;

import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.craftercms.deployer.impl.target.event.DeploymentRuntimeWarningEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.beans.ConstructorProperties;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Cron task that checks the runtime of deployments for all targets.
 * If a deployment has been running longer than the configured threshold,
 * it triggers a {@link DeploymentRuntimeWarningEvent} in the target
 */
@Component
@ConditionalOnProperty("deployer.main.deployments.supervisor.enabled")
public class TargetDeploymentRuntimeSupervisor {
	private static final Logger logger = LoggerFactory.getLogger(TargetDeploymentRuntimeSupervisor.class);

	protected final TargetService targetService;

	@ConstructorProperties({"targetService"})
	public TargetDeploymentRuntimeSupervisor(final TargetService targetService) {
		this.targetService = targetService;
	}

	@Scheduled(cron = "${deployer.main.deployments.supervisor.cron}")
	public void checkDeploymentsRuntime() throws TargetServiceException {
		logger.info("Checking deployments runtime");
		targetService.getAllTargets().forEach(target -> {
			try {
				checkDeploymentRuntime(target);
			} catch (Exception e) {
				logger.error("Failed to check deployment runtime for target: {}", target.getId(), e);
			}
		});
		logger.info("Completed checking deployments runtime");
	}

	/**
	 * Checks the runtime of the current deployment for a target.
	 * If there is a running deployment and its runtime exceeds the configured threshold,
	 * it triggers a {@link DeploymentRuntimeWarningEvent} for the target.
	 *
	 * @param target the target to check
	 */
	protected void checkDeploymentRuntime(Target target) {
		logger.trace("Checking deployment runtime for target: {}", target.getId());
		Deployment currentDeployment = target.getCurrentDeployment();
		if (currentDeployment == null || !currentDeployment.isRunning()) {
			logger.debug("No running deployment found for target: {}", target.getId());
			return;
		}

		ZonedDateTime start = currentDeployment.getStart();
		ZonedDateTime now = ZonedDateTime.now();
		long runtime = start.until(now, ChronoUnit.SECONDS);

		long runtimeThreshold = target.getRuntimeWarningThreshold();

		if (runtime >= runtimeThreshold) {
			long runtimeMillis = start.until(now, ChronoUnit.MILLIS);
			logger.error("Deployment for target {} has been running for {} seconds, which reaches the threshold of {} seconds",
					target.getId(), runtime, runtimeThreshold);
			target.handleEvent(new DeploymentRuntimeWarningEvent(target, currentDeployment, runtimeMillis));
		} else {
			logger.debug("Deployment for target {} is running within the acceptable threshold of {} seconds",
					target.getId(), runtimeThreshold);
		}
		logger.trace("Target: {}, Deployment start: {}, Now: {}, Runtime: {} seconds, Threshold: {} seconds",
				target.getId(), start, now, runtime, runtimeThreshold);
	}
}
