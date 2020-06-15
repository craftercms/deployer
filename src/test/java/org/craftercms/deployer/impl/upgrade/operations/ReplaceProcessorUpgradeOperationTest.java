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

import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.config.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.craftercms.deployer.impl.DeploymentConstants.PROCESSOR_NAME_CONFIG_KEY;
import static org.craftercms.deployer.impl.upgrade.operations.AbstractTargetUpgradeOperation.*;
import static org.junit.Assert.assertEquals;

/**
 * @author joseross
 */
public class ReplaceProcessorUpgradeOperationTest {

    private static final String PROCESSOR = "myProcessor";

    private static final String NEW_PROCESSOR = "myNewProcessor";

    private Map<String, Object> targetConfig;

    private ReplaceProcessorUpgradeOperation processor;

    @Before
    public void setUp() throws ConfigurationException, IOException, org.apache.commons.configuration2.ex.ConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        Resource configFile = new ClassPathResource("upgrade/replaceProcessor/config.yaml");
        try (InputStream is = configFile.getInputStream()) {
            config.read(is);
        }

        Yaml yaml = new Yaml();
        Resource resource = new ClassPathResource("upgrade/replaceProcessor/target.yaml");
        try(InputStream is = resource.getInputStream()) {
            targetConfig = yaml.load(is);
        }

        processor = new ReplaceProcessorUpgradeOperation();
        processor.init(null, null, config);
    }

    @Test
    public void test() {
        processor.doExecute(null, targetConfig);

        assertEquals("instances that do not match shouldn't be replaced", 2, countProcessor(targetConfig, PROCESSOR));
        assertEquals("one processor should be replaced", 1, countProcessor(targetConfig, NEW_PROCESSOR));
        assertEquals("other processors shouldn't be replaced", 1, countProcessor(targetConfig, "otherProcessor"));
    }

    @SuppressWarnings("unchecked")
    private long countProcessor(Map<String, Object> targetConfig, String processor) {
        Map<String, Object> targetObj = (Map<String, Object>)targetConfig.get(CONFIG_KEY_TARGET);
        Map<String, Object> deploymentObj = (Map<String, Object>)targetObj.get(CONFIG_KEY_DEPLOYMENT);
        List<Map<String, Object>> pipelineObj = (List<Map<String, Object>>)deploymentObj.get(CONFIG_KEY_PIPELINE);
        return pipelineObj.stream()
                .filter(processorObj -> processorObj.get(PROCESSOR_NAME_CONFIG_KEY).equals(processor))
                .count();
    }

}
