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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeploymentServiceImpl}.
 *
 * @author avasquez
 */
public class DeploymentServiceImplTest {

    private DeploymentServiceImpl deploymentService;
    private Target foobarTarget;
    private Target barfooTarget;

    @Before
    public void setUp() throws Exception {
        deploymentService = new DeploymentServiceImpl(createTargetService());
    }

    @Test
    public void testDeployAllTargets() throws Exception {
        List<Deployment> deployments = deploymentService.deployAllTargets(false, Collections.emptyMap());

        assertNotNull(deployments);
        assertEquals(2, deployments.size());

        verify(foobarTarget).deploy(eq(false), any());
        verify(barfooTarget).deploy(eq(false), any());
    }

    @Test
    public void testDeployTarget() throws Exception {
        Deployment deployment = deploymentService.deployTarget("test", "foobar", false, Collections.emptyMap());

        assertNotNull(deployment);

        verify(foobarTarget).deploy(eq(false), any());
    }

    private TargetService createTargetService() throws Exception {
        foobarTarget = mock(Target.class);
        barfooTarget = mock(Target.class);

        when(foobarTarget.deploy(eq(false), any())).thenReturn(mock(Deployment.class));
        when(barfooTarget.deploy(eq(false), any())).thenReturn(mock(Deployment.class));

        TargetService targetService = mock(TargetService.class);
        when(targetService.getAllTargets()).thenReturn(Arrays.asList(foobarTarget, barfooTarget));
        when(targetService.getTarget("test", "foobar")).thenReturn(foobarTarget);

        return targetService;
    }

}
