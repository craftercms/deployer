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
package org.craftercms.deployer.impl.upgrade.operations;

import org.craftercms.commons.upgrade.impl.UpgradeContext;
import org.craftercms.commons.config.DisableClassLoadingConstructor;
import org.craftercms.commons.upgrade.impl.operations.AbstractUpgradeOperation;
import org.craftercms.deployer.api.Target;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Base class for all {@link org.craftercms.commons.upgrade.UpgradeOperation}s that handle target configurations
 *
 * @author joseross
 * @since 3.1.8
 */
public abstract class AbstractTargetUpgradeOperation extends AbstractUpgradeOperation<Target> {

    public static final String CONFIG_KEY_PROCESSOR = "processor";
    public static final String CONFIG_KEY_REPLACE = "replace";
    public static final String CONFIG_KEY_REMOVE = "remove";
    public static final String CONFIG_KEY_PROPERTY = "property";
    public static final String CONFIG_KEY_PATTERN = "pattern";
    public static final String CONFIG_KEY_EXPRESSION = "expression";
    public static final String CONFIG_KEY_ADD = "add";
    public static final String CONFIG_KEY_VALUE = "value";
    public static final String CONFIG_KEY_VALUES = "values";

    public static final String CONFIG_KEY_TARGET = "target";
    public static final String CONFIG_KEY_DEPLOYMENT = "deployment";
    public static final String CONFIG_KEY_PIPELINE = "pipeline";
    public static final String CONFIG_KEY_PROCESSOR_NAME = "processorName";
    public static final String CONFIG_KEY_LIFECYCLE_HOOKS = "lifecycleHooks";
    public static final String CONFIG_KEY_HOOK_NAME = "hookName";

    protected Yaml yaml;

    public AbstractTargetUpgradeOperation() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        yaml = new Yaml(new DisableClassLoadingConstructor(), new Representer(), options);
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> getPipeline(Map<String, Object> targetConfig) {
        Map<String, Object> targetObj = (Map<String, Object>)targetConfig.get(CONFIG_KEY_TARGET);
        Map<String, Object> deploymentObj = (Map<String, Object>)targetObj.get(CONFIG_KEY_DEPLOYMENT);
        return (List<Map<String, Object>>)deploymentObj.get(CONFIG_KEY_PIPELINE);
    }

    @Override
    protected void doExecute(UpgradeContext<Target> context) throws Exception {
        var target = context.getTarget();
        Path file = target.getConfigurationFile().toPath();
        Map<String, Object> targetConfig;

        logger.debug("Loading target configuration for {}", target.getId());
        try (InputStream is = Files.newInputStream(file)) {
            targetConfig = yaml.load(is);
        }

        doExecute(target, targetConfig);

        logger.debug("Writing target configuration for {}", target.getId());
        try (Writer writer = Files.newBufferedWriter(file)) {
            yaml.dump(targetConfig, writer);
        }
    }

    protected abstract void doExecute(Target target, Map<String, Object> targetConfig) throws Exception;

}
