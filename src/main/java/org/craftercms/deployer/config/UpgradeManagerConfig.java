/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.deployer.config;

import org.craftercms.commons.upgrade.UpgradeOperation;
import org.craftercms.commons.upgrade.UpgradePipelineFactory;
import org.craftercms.commons.upgrade.VersionProvider;
import org.craftercms.commons.upgrade.impl.pipeline.DefaultUpgradePipelineFactoryImpl;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.impl.upgrade.TargetVersionProvider;
import org.craftercms.deployer.impl.upgrade.operations.ElasticsearchIndexUpgradeOperation;
import org.craftercms.deployer.impl.upgrade.operations.ProcessorUpgradeOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;

/**
 * Holds all configurations related to the Upgrade Manager
 *
 * @author joseross
 * @since 3.1.5
 */
@Configuration
public class UpgradeManagerConfig {

    @Bean
    public VersionProvider versionProvider(@Value("${deployer.main.upgrade.pipelines.target.defaultVersion}")
                                                   String defaultVersion) {
        TargetVersionProvider versionProvider = new TargetVersionProvider();
        versionProvider.setDefaultValue(defaultVersion);
        return versionProvider;
    }

    @Bean
    public UpgradePipelineFactory<Target> upgradePipelineFactory(
            @Autowired VersionProvider versionProvider,
            @Value("${deployer.main.upgrade.configuration}") Resource configurationFile,
            @Value("${deployer.main.upgrade.pipelines.target.name}") String pipelineName) {
        return new DefaultUpgradePipelineFactoryImpl<>(pipelineName, configurationFile, versionProvider);
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public UpgradeOperation<Target> processorUpgrader() {
        return new ProcessorUpgradeOperation();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public UpgradeOperation<Target> elasticsearchIndexUpgrader(
            @Value("${deployer.main.upgrade.operations.elasticsearchIndexUpgrade.enabled}") boolean enabled) {
        ElasticsearchIndexUpgradeOperation operation = new ElasticsearchIndexUpgradeOperation();
        operation.setEnabled(enabled);
        return operation;
    }

}
