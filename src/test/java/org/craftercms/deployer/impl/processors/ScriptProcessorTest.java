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
package org.craftercms.deployer.impl.processors;

import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.craftercms.deployer.api.ChangeSet;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.Target;
import org.jenkinsci.plugins.scriptsecurity.sandbox.blacklists.Blacklist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SandboxInterceptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.StringReader;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author joseross
 */
@RunWith(MockitoJUnitRunner.class)
public class ScriptProcessorTest {

    public static final String UPDATED_FILE = "/site/website/about-us.xml";

    @Mock
    ApplicationContext applicationContext;

    @Mock
    Target target;

    private ScriptProcessor scriptProcessor;

    @Before
    public void setUp() throws IOException {
        GroovyScriptEngine scriptEngine = setUpGroovyScriptEngine();
        SandboxInterceptor sandboxInterceptor = setUpSandboxInterceptor();

        scriptProcessor = new ScriptProcessor(scriptEngine, sandboxInterceptor);
        scriptProcessor.setBeanName("testScriptProcessor");
        scriptProcessor.setTargetId("test");
        scriptProcessor.setApplicationContext(applicationContext);
        scriptProcessor.failDeploymentOnFailure = true;
    }

    protected GroovyScriptEngine setUpGroovyScriptEngine() throws IOException {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        compilerConfig.addCompilationCustomizers(new SandboxTransformer());
        return new GroovyScriptEngine("src/test/resources/processors/scripts",
                new GroovyClassLoader(getClass().getClassLoader(), compilerConfig));
    }

    protected SandboxInterceptor setUpSandboxInterceptor() throws IOException {
        // Empty blacklist, we can test with the basic restrictions
        return new SandboxInterceptor(new Blacklist(new StringReader("")));
    }

    @Test
    public void changeSetIsUpdated() {
        scriptProcessor.scriptPath = "test-changes.groovy";

        when(applicationContext.getBean("testService")).thenReturn(singletonMap("updatedFile", UPDATED_FILE));

        Deployment deployment = new Deployment(target);
        deployment.start();

        ChangeSet changeSet = new ChangeSet();
        changeSet.addCreatedFile("/site/website/index.xml");
        deployment.setChangeSet(changeSet);

        scriptProcessor.execute(deployment);

        assertFalse(deployment.getChangeSet().getUpdatedFiles().isEmpty());
        assertTrue(deployment.getChangeSet().getUpdatedFiles().contains(UPDATED_FILE));
    }

    @Test
    public void scriptRunsInSandbox() {
        scriptProcessor.scriptPath = "test-sandbox.groovy";

        Deployment deployment = new Deployment(target);
        deployment.start();

        ChangeSet changeSet = new ChangeSet();
        changeSet.addCreatedFile("/site/website/index.xml");
        deployment.setChangeSet(changeSet);

        scriptProcessor.execute(deployment);

        assertEquals(Deployment.Status.FAILURE, deployment.getStatus());
    }

    @Test
    public void incompatibleObjectIsReturned() {
        scriptProcessor.scriptPath = "test-return.groovy";

        Deployment deployment = new Deployment(target);
        deployment.start();

        ChangeSet changeSet = new ChangeSet();
        changeSet.addCreatedFile("/site/website/index.xml");
        deployment.setChangeSet(changeSet);

        scriptProcessor.execute(deployment);

        assertEquals(Deployment.Status.FAILURE, deployment.getStatus());
    }

    @Test
    public void nullIsReturned() {
        scriptProcessor.scriptPath = "test-null.groovy";

        Deployment deployment = new Deployment(target);
        deployment.start();

        ChangeSet changeSet = new ChangeSet();
        changeSet.addCreatedFile("/site/website/index.xml");
        deployment.setChangeSet(changeSet);

        scriptProcessor.execute(deployment);

        assertNotEquals(Deployment.Status.FAILURE, deployment.getStatus());
    }

}
