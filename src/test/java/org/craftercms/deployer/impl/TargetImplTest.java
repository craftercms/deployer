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

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Executors;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.craftercms.commons.config.ConfigurationException;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentPipeline;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetNotReadyException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TargetImpl}.
 *
 * @author avasquez
 */
public class TargetImplTest {

    private static final String TEST_ENV = "test";
    private static final String TEST_SITE_NAME = "test";

    private volatile int count;
    private TargetImpl target;

    @Before
    public void setUp() throws Exception {
        count = 0;
        target = new TargetImpl(ZonedDateTime.now(), TEST_ENV, TEST_SITE_NAME, null, null, createConfig(), null,
            Executors.newSingleThreadExecutor(), null, createTargetLifecycleHooksResolver(),
            createDeploymentPipelineFactory(), false);
    }

    @Test
    public void testDeploy() throws Exception {
        try {
            target.deploy(false, new HashMap<>());
            fail("TargetNotReadyException expected");
        } catch (TargetNotReadyException e) {
            // All good
        }

        target.init();

        Deployment dep1 = target.deploy(false, new HashMap<>());
        Deployment dep2 = target.deploy(false, new HashMap<>());
        Deployment dep3 = target.deploy(false, new HashMap<>());

        assertEquals(3, target.getAllDeployments().size());

        Thread.sleep(7000);

        assertNotNull(dep1.getEnd());
        assertEquals(Deployment.Status.SUCCESS, dep1.getStatus());
        assertNotNull(dep2.getEnd());
        assertEquals(Deployment.Status.SUCCESS, dep2.getStatus());
        assertNotNull(dep3.getEnd());
        assertEquals(Deployment.Status.SUCCESS, dep3.getStatus());
        assertEquals(3, count);
    }

    @SuppressWarnings("unchecked")
    private HierarchicalConfiguration<ImmutableNode> createConfig() {
        return mock(HierarchicalConfiguration.class);
    }

    private TargetLifecycleHooksResolver createTargetLifecycleHooksResolver()
            throws DeployerException, ConfigurationException {
        TargetLifecycleHooksResolver resolver = mock(TargetLifecycleHooksResolver.class);
        when(resolver.getHooks(any(), any(), anyString())).thenReturn(Collections.emptyList());

        return resolver;
    }

    private DeploymentPipelineFactory createDeploymentPipelineFactory()
            throws DeployerException, ConfigurationException {
        DeploymentPipeline pipeline = createDeploymentPipeline();
        DeploymentPipelineFactory factory = mock(DeploymentPipelineFactory.class);

        when(factory.getPipeline(any(), any(), anyString())).thenReturn(pipeline);

        return factory;
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
