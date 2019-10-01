/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.springmvc.SpringTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.craftercms.search.elasticsearch.ElasticsearchAdminService;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.search.service.AdminService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.TaskScheduler;

import static org.craftercms.deployer.impl.DeploymentConstants.CREATE_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TargetServiceImplTest}.
 *
 * @author avasquez
 */
public class TargetServiceImplTest {

    private TargetServiceImpl targetService;
    private File targetsFolder;
    private List<TargetLifecycleHook> createHooks;

    @Before
    public void setUp() throws Exception {
        targetsFolder = createTargetsFolder();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("elasticsearchAdminService", mock(ElasticsearchAdminService.class));
        factory.registerSingleton("adminService", mock(AdminService.class));

        GenericApplicationContext context = new GenericApplicationContext(factory);
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(context);
        reader.loadBeanDefinitions(new ClassPathResource("test-application-context.xml"));
        context.refresh();

        targetService = new TargetServiceImpl(
            targetsFolder,
            new ClassPathResource("test-base-target.yaml"),
            new ClassPathResource("test-base-target-override.yaml"),
            new ClassPathResource("test-base-target-context.xml"),
            new ClassPathResource("test-base-target-context-override.xml"),
            "test",
            createHandlebars(),
            context,
            createDeploymentPipelineFactory(),
            createTaskScheduler(),
            createTaskExecutor(),
            createProcessedCommitsStore(),
            createTargetLifecycleHooksResolver());
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.forceDelete(targetsFolder);
    }

    @Test
    public void testResolveTargets() throws Exception {
        List<Target> targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target = targets.get(0);

        assertEquals("test", target.getEnv());
        assertEquals("foobar", target.getSiteName());
        assertEquals("foobar-test", target.getId());
    }

    @Test
    public void testResolveTargetsNoConfigModified() throws Exception {
        List<Target> targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target1 = targets.get(0);

        Thread.sleep(100);

        targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target2 = targets.get(0);

        assertEquals(target1.getLoadDate(), target2.getLoadDate());
    }

    @Test
    public void testResolveTargetsYamlModified() throws Exception {
        List<Target> targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target1 = targets.get(0);

        Thread.sleep(1000);

        FileUtils.touch(new File(targetsFolder, "foobar-test.yaml"));

        targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target2 = targets.get(0);

        assertNotEquals(target1.getLoadDate(), target2.getLoadDate());
    }

    @Test
    public void testResolveTargetsContextModified() throws Exception {
        List<Target> targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target1 = targets.get(0);

        Thread.sleep(1000);

        FileUtils.touch(new File(targetsFolder, "foobar-test-context.xml"));

        targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target2 = targets.get(0);

        assertNotEquals(target1.getLoadDate(), target2.getLoadDate());
    }

    @Test
    public void testGetTarget() throws Exception {
        List<Target> targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target1 = targets.get(0);

        Target target2 = targetService.getTarget(target1.getEnv(), target1.getSiteName());

        assertNotNull(target2);
        assertEquals(target1, target2);
    }

    @Test
    public void testGetAllTargets() throws Exception {
        List<Target> targets1 = targetService.resolveTargets();

        assertEquals(1, targets1.size());

        List<Target> targets2 = targetService.getAllTargets();

        assertEquals(targets1, targets2);
    }

    @Test
    public void testCreateTarget() throws Exception {
        String env = "test";
        String siteName = "barfoo";
        String randomParam = RandomStringUtils.randomAlphanumeric(8);
        Map<String, Object> params = Collections.singletonMap("random_param", randomParam);

        Target target = targetService.createTarget(env, siteName, true, "test", true, params);

        assertNotNull(target);
        assertEquals(env, target.getConfiguration().getString(DeploymentConstants.TARGET_ENV_CONFIG_KEY));
        assertEquals(siteName, target.getConfiguration().getString(DeploymentConstants.TARGET_SITE_NAME_CONFIG_KEY));
        assertEquals(randomParam, target.getConfiguration().getString("target.randomParam"));
        verify(createHooks.get(0)).execute(target);
    }

    @Test
    public void testDeleteTarget() throws Exception {
        List<Target> targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        targetService.deleteTarget("test", "foobar");

        targets = targetService.resolveTargets();

        assertEquals(0, targets.size());
    }

    private File createTargetsFolder() throws IOException {
        File tempTargetsFolder = Files.createTempDirectory("targets").toFile();
        File classpathTargetsFolder = new ClassPathResource("targets").getFile();

        FileUtils.copyDirectory(classpathTargetsFolder, tempTargetsFolder);

        return tempTargetsFolder;
    }

    private DeploymentPipelineFactory createDeploymentPipelineFactory() throws ConfigurationException,
                                                                               DeployerException {
        DeploymentPipelineFactory pipelineFactory = mock(DeploymentPipelineFactory.class);
        when(pipelineFactory.getPipeline(any(), any(), anyString())).thenReturn(mock(DeploymentPipeline.class));

        return pipelineFactory;
    }

    private TaskScheduler createTaskScheduler() {
        return mock(TaskScheduler.class);
    }

    private ExecutorService createTaskExecutor() {
        return mock(ExecutorService.class);
    }

    private ProcessedCommitsStore createProcessedCommitsStore() {
        return mock(ProcessedCommitsStore.class);
    }

    private TargetLifecycleHooksResolver createTargetLifecycleHooksResolver() throws ConfigurationException,
                                                                                     DeployerException {
        createHooks = Collections.singletonList(mock(TargetLifecycleHook.class));

        TargetLifecycleHooksResolver resolver = mock(TargetLifecycleHooksResolver.class);
        when(resolver.getHooks(any(), any(), eq(CREATE_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY))).thenReturn(createHooks);

        return resolver;
    }

    private Handlebars createHandlebars() {
        SpringTemplateLoader templateLoader = new SpringTemplateLoader(new DefaultResourceLoader());
        templateLoader.setPrefix("classpath:templates/targets");
        templateLoader.setSuffix("-target-template.yaml");

        Handlebars handlebars = new Handlebars(templateLoader);
        handlebars.prettyPrint(true);

        return handlebars;
    }

}
