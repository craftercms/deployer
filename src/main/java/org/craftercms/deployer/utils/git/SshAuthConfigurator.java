package org.craftercms.deployer.utils.git;

import com.jcraft.jsch.Session;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;

/**
 * Created by alfonso on 4/19/17.
 */
public class SshAuthConfigurator implements GitAuthenticationConfigurator {

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
