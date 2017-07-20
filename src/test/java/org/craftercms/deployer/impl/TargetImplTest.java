package org.craftercms.deployer.impl;

import java.util.HashMap;

import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Created by alfonso on 7/14/17.
 */
public class TargetImplTest {

    private static final String TEST_ENV = "test";
    private static final String TEST_SITE_NAME = "test";

    private volatile int count;
    private TargetImpl target;

    @Before
    public void setUp() throws Exception {
        count = 0;
        target = new TargetImpl(TEST_ENV, TEST_SITE_NAME, TEST_ENV + "-" + TEST_SITE_NAME, createDeploymentPipeline(), null, null, null);
    }

    @Test
    public void testDeploy() throws Exception {
        target.deploy(new HashMap<>());
        target.deploy(new HashMap<>());
        target.deploy(new HashMap<>());

        assertEquals(3, target.getAllDeployments().size());

        Thread.sleep(7000);

        assertEquals(3, count);
    }

    private DeploymentPipeline createDeploymentPipeline() {
        DeploymentPipeline pipeline = mock(DeploymentPipeline.class);
        doAnswer(invocationOnMock -> {
            Deployment deployment = (Deployment)invocationOnMock.getArguments()[0];
            deployment.start();

            int currentCount = ++count;

            Thread.sleep(2000);

            assertEquals(currentCount, count);

            deployment.end(Deployment.Status.SUCCESS);

            return null;
        }).when(pipeline).execute(any(Deployment.class));

        return pipeline;
    }

}
