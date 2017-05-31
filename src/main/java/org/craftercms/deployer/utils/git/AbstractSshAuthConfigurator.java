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
