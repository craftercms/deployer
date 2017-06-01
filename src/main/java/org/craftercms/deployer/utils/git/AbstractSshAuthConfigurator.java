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
package org.craftercms.deployer.utils.git;

import com.jcraft.jsch.Session;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;

/**
 * {@link GitAuthenticationConfigurator} that configures the {@code TransportCommand} to use SSH, but without providing
 * any authentication functionality. Actual authentication functionality is provided by subclasses.
 *
 * @author avasquez
 */
public abstract class AbstractSshAuthConfigurator implements GitAuthenticationConfigurator {

    @Override
    public void configureAuthentication(TransportCommand command) {
        SshSessionFactory sessionFactory = createSessionFactory();

        command.setTransportConfigCallback(transport -> ((SshTransport) transport).setSshSessionFactory(sessionFactory));
    }

    protected SshSessionFactory createSessionFactory() {
        return new JschConfigSessionFactory() {

            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                // Do nothing
            }

        };
    }

}
