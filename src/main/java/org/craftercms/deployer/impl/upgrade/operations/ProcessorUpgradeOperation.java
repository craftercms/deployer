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

import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.upgrade.impl.operations.AbstractUpgradeOperation;
import org.craftercms.deployer.api.Target;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;
import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSOR_NAME_CONFIG_KEY;

/**
 * Implementation of {@link org.craftercms.commons.upgrade.UpgradeOperation} that handles target configurations
 *
 * @author joseross
 * @since 3.1.5
 */
public class ProcessorUpgradeOperation extends AbstractUpgradeOperation<Target> {

    public static final String CONFIG_KEY_PROCESSOR = "processor";
    public static final String CONFIG_KEY_REPLACE = "replace";
    public static final String CONFIG_KEY_PROPERTY = "property";
    public static final String CONFIG_KEY_PATTERN = "pattern";
    public static final String CONFIG_KEY_EXPRESSION = "expression";

    public static final String CONFIG_KEY_TARGET = "target";
    public static final String CONFIG_KEY_DEPLOYMENT = "deployment";
    public static final String CONFIG_KEY_PIPELINE = "pipeline";

    /**
     * The name of the processor to update
     */
    protected String processorName;

    /**
     * The properties to replace
     */
    protected List<Map<String, String>> replacements;

    protected Yaml yaml;

    public ProcessorUpgradeOperation() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        yaml = new Yaml(options);
    }

    @Override
    protected void doInit(final HierarchicalConfiguration<?> config) throws ConfigurationException {
        processorName = getRequiredStringProperty(config, CONFIG_KEY_PROCESSOR);
        replacements = new LinkedList<>();
        for (HierarchicalConfiguration<?> replacementConfig : config.configurationsAt(CONFIG_KEY_REPLACE)) {
            Map<String, String> map = new HashMap<>();
            map.put(CONFIG_KEY_PROPERTY, getRequiredStringProperty(replacementConfig, CONFIG_KEY_PROPERTY));
            map.put(CONFIG_KEY_PATTERN, getRequiredStringProperty(replacementConfig, CONFIG_KEY_PATTERN));
            map.put(CONFIG_KEY_EXPRESSION, getRequiredStringProperty(replacementConfig, CONFIG_KEY_EXPRESSION));
            replacements.add(map);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doExecute(final Target target) throws Exception {
        Path file = target.getConfigurationFile().toPath();
        Map<String, Object> targetConfig;
        try (InputStream is = Files.newInputStream(file)) {
            targetConfig = yaml.load(is);
        }
        Map<String, Object> targetObj = (Map<String, Object>)targetConfig.get(CONFIG_KEY_TARGET);
        Map<String, Object> deploymentObj = (Map<String, Object>)targetObj.get(CONFIG_KEY_DEPLOYMENT);
        List<Map<String, Object>> pipelineObj = (List<Map<String, Object>>)deploymentObj.get(CONFIG_KEY_PIPELINE);
        pipelineObj.stream()
            .filter(processor -> processorName.equals(processor.get(PROCESSOR_NAME_CONFIG_KEY))).forEach(processor -> {
            logger.debug("Running replacements for processor '{}' in target '{}'", processorName, target);
            replacements.forEach(config -> {
                String property = config.get(CONFIG_KEY_PROPERTY);
                String pattern = config.get(CONFIG_KEY_PATTERN);
                String expression = config.get(CONFIG_KEY_EXPRESSION);
                logger.trace("Replacing property '{}' for processor '{}' in target '{}'", property, processorName,
                    target);
                processor.put(property, processor.get(property).toString().replaceAll(pattern, expression));
            });
        });
        try (Writer writer = Files.newBufferedWriter(file)) {
            yaml.dump(targetConfig, writer);
        }
    }

}
