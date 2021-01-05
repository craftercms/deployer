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
package org.craftercms.deployer.utils.scripting;

import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.RejectASTTransformsCustomizer;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Implementation of {@link org.springframework.beans.factory.FactoryBean} for {@link GroovyScriptEngine}
 *
 * @author joseross
 * @since 3.1.12
 */
public class ScriptEngineFactory extends AbstractFactoryBean<GroovyScriptEngine> {

    /**
     * List of relative paths to load scripts
     */
    protected String[] urls;

    /**
     * Indicates if the sandbox should be enabled
     */
    protected boolean sandboxEnabled;

    public ScriptEngineFactory(String[] urls, boolean sandboxEnabled) {
        this.urls = urls;
        this.sandboxEnabled = sandboxEnabled;
    }

    @Override
    public Class<?> getObjectType() {
        return GroovyScriptEngine.class;
    }

    @Override
    protected GroovyScriptEngine createInstance() throws Exception {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        if (sandboxEnabled) {
            compilerConfig.addCompilationCustomizers(new RejectASTTransformsCustomizer(), new SandboxTransformer());
        }
        return new GroovyScriptEngine(urls, new GroovyClassLoader(getClass().getClassLoader(), compilerConfig));
    }

}
