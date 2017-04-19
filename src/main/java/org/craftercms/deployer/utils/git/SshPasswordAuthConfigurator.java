package org.craftercms.deployer.utils.git;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.util.FS;
import org.springframework.beans.factory.annotation.Required;

/**
 * Created by alfonso on 4/19/17.
 */
public class SshPasswordAuthConfigurator extends SshAuthConfigurator {

    protected String password;

    public SshPasswordAuthConfigurator(String password) {
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
