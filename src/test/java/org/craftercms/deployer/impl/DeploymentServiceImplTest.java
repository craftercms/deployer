/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
        List<Deployment> deployments = deploymentService.deployAllTargets(Collections.emptyMap());

        assertNotNull(deployments);
        assertEquals(2, deployments.size());

        verify(foobarTarget).deploy(any());
        verify(barfooTarget).deploy(any());
    }

    @Test
    public void testDeployTarget() throws Exception {
        Deployment deployment = deploymentService.deployTarget("test", "foobar", Collections.emptyMap());

        assertNotNull(deployment);

        verify(foobarTarget).deploy(any());
    }

    private TargetService createTargetService() throws Exception {
        foobarTarget = mock(Target.class);
        barfooTarget = mock(Target.class);

        when(foobarTarget.deploy(any())).thenReturn(mock(Deployment.class));
        when(barfooTarget.deploy(any())).thenReturn(mock(Deployment.class));

        TargetService targetService = mock(TargetService.class);
        when(targetService.getAllTargets()).thenReturn(Arrays.asList(foobarTarget, barfooTarget));
        when(targetService.getTarget("test", "foobar")).thenReturn(foobarTarget);

        return targetService;
    }

}
