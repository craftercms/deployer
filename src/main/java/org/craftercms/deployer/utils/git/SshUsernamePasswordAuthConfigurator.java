package org.craftercms.deployer.utils.git;

import com.jcraft.jsch.Session;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;

/**
 * {@link GitAuthenticationConfigurator} that configures the {@code TransportCommand} to use SSH with username/password authentication.
 * The user name is expected to be part of the Git SSH URL, while the password is provided separately and injected to this class.
 *
 * @author avasquez
 */
public class SshUsernamePasswordAuthConfigurator extends AbstractSshAuthConfigurator {

    protected String password;

    public SshUsernamePasswordAuthConfigurator(String password) {
        this.password = password;
    }

    @Override
    protected SshSessionFactory createSessionFactory() {
        return new JschConfigSessionFactory() {

            @Override
            protected void configure(OpenSshConfig.Host hc, Session session) {
                session.setPassword(password);
            }


        };
    }

}
