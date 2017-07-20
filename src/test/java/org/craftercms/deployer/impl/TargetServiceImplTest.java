package org.craftercms.deployer.impl;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.Target;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.TaskScheduler;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by alfonso on 7/14/17.
 */
public class TargetServiceImplTest {


    private TargetServiceImpl targetService;

    @Before
    public void setUp() throws Exception {
        DeploymentPipelineFactory pipelineFactory = mock(DeploymentPipelineFactory.class);
        TaskScheduler taskScheduler = mock(TaskScheduler.class);
        ProcessedCommitsStore commitsStore = mock(ProcessedCommitsStore.class);

        when(pipelineFactory.getPipeline(any(), any(), anyString())).thenReturn(mock(DeploymentPipeline.class));

        targetService = new TargetServiceImpl(
            new ClassPathResource("targets").getFile(),
            new ClassPathResource("test-base-target.yaml"),
            new ClassPathResource("test-base-target-override.yaml"),
            new ClassPathResource("test-base-target-context.xml"),
            new ClassPathResource("test-base-target-context-override.xml"),
            null,
            null,
            new ClassPathXmlApplicationContext("test-application-context.xml"),
            pipelineFactory,
            taskScheduler,
            commitsStore);
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

        File yamlFile = new ClassPathResource("targets/foobar-test.yaml").getFile();
        FileUtils.touch(yamlFile);

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

        File contextFile = new ClassPathResource("targets/foobar-test-context.xml").getFile();
        FileUtils.touch(contextFile);

        targets = targetService.resolveTargets();

        assertEquals(1, targets.size());

        Target target2 = targets.get(0);

        assertNotEquals(target1.getLoadDate(), target2.getLoadDate());
    }

}
