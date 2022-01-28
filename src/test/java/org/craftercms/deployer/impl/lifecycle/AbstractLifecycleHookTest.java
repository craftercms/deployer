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
package org.craftercms.deployer.impl.lifecycle;

import org.apache.commons.configuration2.Configuration;
import org.craftercms.deployer.api.Target;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

/**
 * @author joseross
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractLifecycleHookTest {

    @Spy
    private AbstractLifecycleHook hook;

    @Mock
    private Configuration config;

    @Mock
    private Target target;

    @Test
    public void hookEnabledTest() throws Exception {
        when(config.getBoolean(AbstractLifecycleHook.CONFIG_KEY_DISABLED, false)).thenReturn(false);

        hook.init(config);
        hook.execute(target);

        verify(hook).doExecute(eq(target));
    }

    @Test
    public void hookDisabledTest() throws Exception {
        when(config.getBoolean(AbstractLifecycleHook.CONFIG_KEY_DISABLED, false)).thenReturn(true);

        hook.init(config);
        hook.execute(target);

        verify(hook, never()).doExecute(eq(target));
    }

}
