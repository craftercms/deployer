/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.impl.processors;

import groovy.lang.Binding;
import groovy.util.GroovyScriptEngine;
import org.apache.commons.configuration2.Configuration;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.ProcessorExecution;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.beans.ConstructorProperties;

import static org.craftercms.commons.config.ConfigUtils.getRequiredStringProperty;

/**
 * Implementation of {@link org.craftercms.deployer.api.DeploymentProcessor} that delegates execution to a Groovy script
 * Can be configured with the following YAML properties:
 *
 * <ul>
 *   <li><strong>scriptPath:</strong> The relative path of the script to execute</li>
 * </ul>
 *
 * @author joseross
 * @since 3.1.12
 */
public class ScriptProcessor extends AbstractMainDeploymentProcessor implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(ScriptProcessor.class);

    public static final String CONFIG_KEY_SCRIPT_PATH = "scriptPath";

    public static final String SCRIPT_VAR_LOGGER = "logger";
    public static final String SCRIPT_VAR_APP_CTX = "applicationContext";
    public static final String SCRIPT_VAR_DEPLOYMENT = "deployment";
    public static final String SCRIPT_VAR_EXECUTION = "execution";
    public static final String SCRIPT_VAR_FILTERED_CHANGE_SET = "filteredChangeSet";
    public static final String SCRIPT_VAR_ORIGINAL_CHANGE_SET = "originalChangeSet";

    protected ApplicationContext applicationContext;

    protected final GroovyScriptEngine scriptEngine;

    protected final SandboxInterceptor sandboxInterceptor;

    // Config properties (populated on init)

    /**
     * The relative path of the script to execute
     */
    protected String scriptPath;

    @ConstructorProperties({"scriptEngine", "sandboxInterceptor"})
    public ScriptProcessor(GroovyScriptEngine scriptEngine, SandboxInterceptor sandboxInterceptor) {
        this.scriptEngine = scriptEngine;
        this.sandboxInterceptor = sandboxInterceptor;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    protected void doInit(Configuration config) throws ConfigurationException {
        scriptPath = getRequiredStringProperty(config, CONFIG_KEY_SCRIPT_PATH);
    }

    @Override
    protected void doDestroy() {
        // do nothing
    }

    @Override
    protected ChangeSet doMainProcess(Deployment deployment, ProcessorExecution execution, ChangeSet filteredChangeSet,
                                      ChangeSet originalChangeSet) throws DeployerException {
        if (sandboxInterceptor != null) {
            sandboxInterceptor.register();
        }
        try {
            Binding binding = new Binding();
            binding.setVariable(SCRIPT_VAR_LOGGER, logger);
            binding.setVariable(SCRIPT_VAR_APP_CTX, applicationContext);
            binding.setVariable(SCRIPT_VAR_DEPLOYMENT, deployment);
            binding.setVariable(SCRIPT_VAR_EXECUTION, execution);
            binding.setVariable(SCRIPT_VAR_FILTERED_CHANGE_SET, filteredChangeSet);
            binding.setVariable(SCRIPT_VAR_ORIGINAL_CHANGE_SET, originalChangeSet);

            logger.info("Starting execution of script {}", scriptPath);
            Object result = scriptEngine.run(scriptPath, binding);
            logger.info("Completed execution of script {}", scriptPath);

            if (result != null &&!ChangeSet.class.isAssignableFrom(result.getClass())) {
                throw new DeployerException("Incompatible type " + result.getClass().getName() +
                        " returned by script " + scriptPath);
            }

            return (ChangeSet) result;
        } catch (Throwable e) {
            throw new DeployerException("Error executing script " + scriptPath, e);
        } finally {
            if (sandboxInterceptor != null) {
                sandboxInterceptor.unregister();
            }
        }
    }

}
