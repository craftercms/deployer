/*
 * Copyright (C) 2007-2022 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.utils.core;

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.impl.TargetImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.craftercms.commons.config.PublishingTargetResolver.LIVE;
import static org.craftercms.commons.config.PublishingTargetResolver.PREVIEW;
import static org.craftercms.commons.config.PublishingTargetResolver.STAGING;
import static org.craftercms.deployer.api.Target.AUTHORING_ENV;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author joseross
 */
@RunWith(MockitoJUnitRunner.class)
public class TargetAwarePublishingTargetResolverTest {

    public static final String SITE_NAME = "mySite";

    public static final String STAGING_PATTERN = ".+-staging";

    public static final String STAGING_SITE_NAME = SITE_NAME + "-staging";

    @Mock
    private Target stagingTarget;

    @Mock
    private Target authoringTarget;

    @Mock
    private Target previewTarget;

    @Mock
    private Target liveTarget;

    private TargetAwarePublishingTargetResolver resolver;

    @Before
    public void setUp() {
        when(stagingTarget.getSiteName()).thenReturn(STAGING_SITE_NAME);

        when(authoringTarget.getSiteName()).thenReturn(SITE_NAME);
        when(authoringTarget.getEnv()).thenReturn(AUTHORING_ENV);

        when(previewTarget.getSiteName()).thenReturn(SITE_NAME);
        when(previewTarget.getEnv()).thenReturn(PREVIEW);

        when(liveTarget.getSiteName()).thenReturn(SITE_NAME);
        when(liveTarget.getEnv()).thenReturn("aws");

        resolver = new TargetAwarePublishingTargetResolver(STAGING_PATTERN);
    }

    @Test
    public void testStaging() {
        TargetImpl.setCurrent(stagingTarget);

        String target = resolver.getPublishingTarget();
        assertEquals(target, STAGING);

        TargetImpl.clear();
    }

    @Test
    public void testAuthoring() {
        TargetImpl.setCurrent(authoringTarget);

        String target = resolver.getPublishingTarget();
        assertEquals(target, PREVIEW);

        TargetImpl.clear();
    }

    @Test
    public void testPreview() {
        TargetImpl.setCurrent(previewTarget);

        String target = resolver.getPublishingTarget();
        assertEquals(target, PREVIEW);

        TargetImpl.clear();
    }

    @Test
    public void testLive() {
        TargetImpl.setCurrent(liveTarget);

        String target = resolver.getPublishingTarget();
        assertEquals(target, LIVE);

        TargetImpl.clear();
    }

}
