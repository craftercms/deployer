/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.springmvc.SpringTemplateLoader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.commons.config.EncryptionAwareConfigurationReader;
import org.craftercms.commons.crypto.impl.NoOpTextEncryptor;
import org.craftercms.commons.upgrade.UpgradeManager;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.craftercms.deployer.api.lifecycle.TargetLifecycleHook;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.TaskScheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.craftercms.deployer.impl.DeploymentConstants.CREATE_TARGET_LIFECYCLE_HOOKS_CONFIG_KEY;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TargetServiceImplTest}.
 *
 * @author avasquez
 */
public class TargetServiceImplTest {
    private static final String RANDOM_PARAM_VARIABLE = "random_param";

    private static final String ENVIRONMENT = "the-env";
    private static final String SOURCE_SITE_NAME = "source-site";
    private static final String EXISTING_SITE = "existing-site";
    private static final String NON_EXISTING_SITE = "non-existing-site";
    private static final String NEW_SITE_NAME = "new-site";
    private static final String TEMPLATE_NAME = "test";
    private static final String TEST_PARAM_1_VARIABLE = "test-param-1";
    private static final String TEST_PARAM_VALUE_1 = "test-value-1";

    private TargetServiceImpl targetService;
    private File targetsFolder;
    private List<TargetLifecycleHook> createHooks;
    private Handlebars handlebars;

    private final ArgumentMatcher<Target> matchesNewTarget = (Target target) -> matches(target, NEW_SITE_NAME, ENVIRONMENT);

    private static boolean matches(final Target target, final String siteName, final String env) {
        assertEquals(siteName, target.getSiteName());
        assertEquals(env, target.getEnv());
        return true;
    }

    @Before
    public void setUp() throws Exception {
        targetsFolder = createTargetsFolder();

        DeploymentPipelineFactory deploymentPipelineFactory = createDeploymentPipelineFactory();
        TaskScheduler taskScheduler = createTaskScheduler();
        ExecutorService taskExecutor = createTaskExecutor();
        ProcessedCommitsStore processedCommitsStore = createProcessedCommitsStore();
        TargetLifecycleHooksResolver targetLifecycleHooksResolver = createTargetLifecycleHooksResolver();

        DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
        factory.registerSingleton("deploymentPipelineFactory", deploymentPipelineFactory);
        factory.registerSingleton("taskScheduler", taskScheduler);
        factory.registerSingleton("taskExecutor", taskExecutor);
        factory.registerSingleton("processedCommitsStore", processedCommitsStore);
        factory.registerSingleton("targetLifecycleHooksResolver", targetLifecycleHooksResolver);

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
            deploymentPipelineFactory,
            taskScheduler,
            taskExecutor,
            processedCommitsStore,
            targetLifecycleHooksResolver,
            createConfigurationReader(),
            createUpgradeManager());
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
        Map<String, Object> params = Collections.singletonMap(RANDOM_PARAM_VARIABLE, randomParam);

        Target target = targetService.createTarget(env, siteName, true, "test", params);

        assertNotNull(target);
        assertEquals(env, target.getConfiguration().getString(DeploymentConstants.TARGET_ENV_CONFIG_KEY));
        assertEquals(siteName, target.getConfiguration().getString(DeploymentConstants.TARGET_SITE_NAME_CONFIG_KEY));
        assertEquals(randomParam, target.getConfiguration().getString("target.randomParam"));
        verify(createHooks.get(0)).execute(target);
    }

    @Test
    public void testInitHooksOnCreateTarget() throws Exception {
        String env = "test";
        String siteName = "barfoo";
        String randomParam = RandomStringUtils.randomAlphanumeric(8);
        Map<String, Object> params = Collections.singletonMap(RANDOM_PARAM_VARIABLE, randomParam);

        TargetServiceImpl targetServiceSpy = Mockito.spy(targetService);
        TargetImpl mockTarget = mock(TargetImpl.class);
        doAnswer(invocationOnMock -> {
            TargetImpl target = (TargetImpl) invocationOnMock.callRealMethod();
            return spy(target);
        }).when(targetServiceSpy).buildTarget(any(), any());
        TargetImpl target = (TargetImpl) targetServiceSpy.createTarget(env, siteName, true, "test", params);

        assertNotNull(target);
        assertEquals(env, target.getConfiguration().getString(DeploymentConstants.TARGET_ENV_CONFIG_KEY));
        assertEquals(siteName, target.getConfiguration().getString(DeploymentConstants.TARGET_SITE_NAME_CONFIG_KEY));
        assertEquals(randomParam, target.getConfiguration().getString("target.randomParam"));
        verify(target).executeCreateHooks();

        InOrder inOrder = inOrder(targetServiceSpy, target);
        ArgumentMatcher<Target> matchesTarget = t -> matches(t, siteName, env);
        inOrder.verify(target).executeCreateHooks();
        inOrder.verify(targetServiceSpy).startInit(argThat(matchesTarget));
        verify(target, never()).executeDuplicateHooks();
    }

    @Test
    public void testDeleteTarget() throws Exception {
        List<Target> targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        targetService.deleteTarget("test", "foobar");

        targets = targetService.resolveTargets();

        assertEquals(0, targets.size());
    }

    @Test
    public void testDuplicateNonExistingTarget() throws TargetServiceException {
        TargetService targetServiceSpy = Mockito.spy(targetService);
        doReturn(false).when(targetServiceSpy).targetExists(ENVIRONMENT, NON_EXISTING_SITE);
        assertThrows(TargetNotFoundException.class,
                () -> targetServiceSpy.duplicateTarget(ENVIRONMENT, NON_EXISTING_SITE, NEW_SITE_NAME, false, "test", emptyMap()));
    }

    @Test
    public void testDuplicateAlreadyExistingTarget() throws TargetServiceException {
        TargetService targetServiceSpy = Mockito.spy(targetService);
        when(targetServiceSpy.targetExists(ENVIRONMENT, EXISTING_SITE)).thenReturn(true);

        assertThrows(TargetAlreadyExistsException.class,
                () -> targetServiceSpy.duplicateTarget(ENVIRONMENT, SOURCE_SITE_NAME, EXISTING_SITE, false, "test", emptyMap()));
    }

    @Test
    public void testDuplicateTarget() throws Exception {
        ObjectId theProcessedCommit = ObjectId.fromString("1234567890123456789012345678901234567890");

        TargetImpl mockSourceTarget = mock(TargetImpl.class);
        when(mockSourceTarget.getId()).thenReturn(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));

        when(targetService.processedCommitsStore.load(mockSourceTarget.getId())).thenReturn(theProcessedCommit);

        TargetImpl mockNewTarget = mock(TargetImpl.class);
        when(mockNewTarget.getId()).thenReturn(TargetImpl.getId(ENVIRONMENT, NEW_SITE_NAME));

        TargetServiceImpl targetServiceSpy = Mockito.spy(targetService);
        doReturn(mockNewTarget).when(targetServiceSpy).buildTarget(any(), any());
        doReturn(mockSourceTarget).when(targetServiceSpy).findLoadedTargetById(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));

        when(targetServiceSpy.targetExists(ENVIRONMENT, NEW_SITE_NAME)).thenReturn(false);

        targetServiceSpy.duplicateTarget(ENVIRONMENT, SOURCE_SITE_NAME, NEW_SITE_NAME, false, "test", new HashMap<>());

        verify(mockNewTarget).executeDuplicateHooks();
    }

    @Test
    public void testInitHooksOnDuplicateTarget() throws Exception {
        ObjectId theProcessedCommit = ObjectId.fromString("1234567890123456789012345678901234567890");

        Target mockSourceTarget = mock(Target.class);
        when(mockSourceTarget.getId()).thenReturn(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));

        when(targetService.processedCommitsStore.load(mockSourceTarget.getId())).thenReturn(theProcessedCommit);

        TargetServiceImpl targetServiceSpy = Mockito.spy(targetService);
        TargetImpl mockNewTarget = mock(TargetImpl.class);
        doReturn(NEW_SITE_NAME).when(mockNewTarget).getSiteName();
        doReturn(ENVIRONMENT).when(mockNewTarget).getEnv();
        doReturn(mockSourceTarget).when(targetServiceSpy).findLoadedTargetById(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));
        doReturn(mockNewTarget).when(targetServiceSpy).buildTarget(any(), any());

        when(targetServiceSpy.targetExists(ENVIRONMENT, NEW_SITE_NAME)).thenReturn(false);

        targetServiceSpy.duplicateTarget(ENVIRONMENT, SOURCE_SITE_NAME, NEW_SITE_NAME, false, "test", new HashMap<>());

        InOrder inOrder = inOrder(targetServiceSpy, mockNewTarget);

        inOrder.verify(mockNewTarget).executeDuplicateHooks();
        inOrder.verify(targetServiceSpy).startInit(argThat(matchesNewTarget));
        verify(mockNewTarget, never()).executeCreateHooks();
    }

    @Test
    public void testDuplicateHooksOnDuplicateTarget() throws Exception {
        ObjectId theProcessedCommit = ObjectId.fromString("1234567890123456789012345678901234567890");

        Target mockSourceTarget = mock(Target.class);
        when(mockSourceTarget.getId()).thenReturn(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));

        when(targetService.processedCommitsStore.load(mockSourceTarget.getId())).thenReturn(theProcessedCommit);

        TargetServiceImpl targetServiceSpy = Mockito.spy(targetService);
        TargetImpl mockNewTarget = mock(TargetImpl.class);
        doReturn(emptyList()).when(mockNewTarget).getDuplicateHooks();
        doCallRealMethod().when(mockNewTarget).executeDuplicateHooks();
        doReturn(NEW_SITE_NAME).when(mockNewTarget).getSiteName();
        doReturn(ENVIRONMENT).when(mockNewTarget).getEnv();
        doReturn(mockSourceTarget).when(targetServiceSpy).findLoadedTargetById(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));
        doReturn(mockNewTarget).when(targetServiceSpy).buildTarget(any(), any());

        when(targetServiceSpy.targetExists(ENVIRONMENT, NEW_SITE_NAME)).thenReturn(false);

        targetServiceSpy.duplicateTarget(ENVIRONMENT, SOURCE_SITE_NAME, NEW_SITE_NAME, false, "test", new HashMap<>());

        InOrder inOrder = inOrder(targetServiceSpy, mockNewTarget);

        inOrder.verify(mockNewTarget).executeDuplicateHooks();
        inOrder.verify(mockNewTarget).getDuplicateHooks();
        inOrder.verify(targetServiceSpy).startInit(argThat(matchesNewTarget));
        verify(mockNewTarget, never()).executeCreateHooks();
    }

    @Test
    public void testDuplicateTargetPassTemplateParams() throws Exception {
        Target mockSourceTarget = mock(Target.class);
        when(mockSourceTarget.getId()).thenReturn(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));

        TargetServiceImpl targetServiceSpy = Mockito.spy(targetService);
        doReturn(mockSourceTarget).when(targetServiceSpy).findLoadedTargetById(TargetImpl.getId(ENVIRONMENT, SOURCE_SITE_NAME));

        when(targetServiceSpy.targetExists(ENVIRONMENT, NEW_SITE_NAME)).thenReturn(false);

        Map<String, Object> templateParams = new HashMap<>();

        String randomParam = RandomStringUtils.randomAlphanumeric(8);
        templateParams.put(RANDOM_PARAM_VARIABLE, randomParam);
        templateParams.put(TEST_PARAM_1_VARIABLE, TEST_PARAM_VALUE_1);
        targetServiceSpy.duplicateTarget(ENVIRONMENT, SOURCE_SITE_NAME, NEW_SITE_NAME, false, TEMPLATE_NAME, templateParams);

        Target target = targetServiceSpy.getTarget(ENVIRONMENT, NEW_SITE_NAME);
        assertEquals(randomParam, target.getConfiguration().getString("target.randomParam"));

        verify(handlebars).compile(TEMPLATE_NAME);
        verify(targetServiceSpy).processConfigTemplate(eq(TEMPLATE_NAME), argThat(params -> {
            if (params instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) params;
                return TEST_PARAM_VALUE_1.equals(map.get(TEST_PARAM_1_VARIABLE));
            }
            return false;
        }), any());
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

    private EncryptionAwareConfigurationReader createConfigurationReader() {
        return new EncryptionAwareConfigurationReader(new NoOpTextEncryptor());
    }

    @SuppressWarnings("unchecked")
    private UpgradeManager<Target> createUpgradeManager() {
        return mock(UpgradeManager.class);
    }

    private Handlebars createHandlebars() {
        SpringTemplateLoader templateLoader = new SpringTemplateLoader(new DefaultResourceLoader());
        templateLoader.setPrefix("classpath:templates/targets");
        templateLoader.setSuffix("-target-template.yaml");

        handlebars = spy(new Handlebars(templateLoader));
        handlebars.prettyPrint(true);

        return handlebars;
    }

}
