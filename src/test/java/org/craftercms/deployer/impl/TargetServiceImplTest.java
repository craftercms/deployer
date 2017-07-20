package org.craftercms.deployer.impl;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.springmvc.SpringTemplateLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by alfonso on 7/14/17.
 */
public class TargetServiceImplTest {

    private TargetServiceImpl targetService;
    private File targetsFolder;

    @Before
    public void setUp() throws Exception {
        targetsFolder = createTargetsFolder();

        targetService = new TargetServiceImpl(
            targetsFolder,
            new ClassPathResource("test-base-target.yaml"),
            new ClassPathResource("test-base-target-override.yaml"),
            new ClassPathResource("test-base-target-context.xml"),
            new ClassPathResource("test-base-target-context-override.xml"),
            "test",
            createHandlebars(),
            new ClassPathXmlApplicationContext("test-application-context.xml"),
            createDeploymentPipelineFactory(),
            createTaskScheduler(),
            createProcessedCommitsStore());
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

        Target target = targetService.createTarget(env, siteName, true, "test", params);

        assertNotNull(target);
        assertEquals(env, target.getConfiguration().getString(DeploymentConstants.TARGET_ENV_CONFIG_KEY));
        assertEquals(siteName, target.getConfiguration().getString(DeploymentConstants.TARGET_SITE_NAME_CONFIG_KEY));
        assertEquals(randomParam, target.getConfiguration().getString("target.randomParam"));
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

    private DeploymentPipelineFactory createDeploymentPipelineFactory() throws DeployerException {
        DeploymentPipelineFactory pipelineFactory = mock(DeploymentPipelineFactory.class);
        when(pipelineFactory.getPipeline(any(), any(), anyString())).thenReturn(mock(DeploymentPipeline.class));

        return pipelineFactory;
    }

    private TaskScheduler createTaskScheduler() {
        return mock(TaskScheduler.class);
    }

    private ProcessedCommitsStore createProcessedCommitsStore() {
        return mock(ProcessedCommitsStore.class);
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
