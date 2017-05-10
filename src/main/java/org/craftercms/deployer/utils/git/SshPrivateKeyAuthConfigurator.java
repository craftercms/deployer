package org.craftercms.deployer.utils.git;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.util.FS;

/**
 * Created by alfonso on 4/19/17.
 */
public class SshPrivateKeyAuthConfigurator extends SshAuthConfigurator {

    protected String privateKeyPath;
    protected String passphrase;

    public SshPrivateKeyAuthConfigurator(String privateKeyPath, String passphrase) {
        this.privateKeyPath = privateKeyPath;
        this.passphrase = passphrase;
    }

    @Override
    protected SshSessionFactory createSessionFactory() {
        return new JschConfigSessionFactory() {

            @Override
            protected JSch createDefaultJSch(FS fs) throws JSchException {
                JSch defaultJSch = super.createDefaultJSch(fs);

                if (StringUtils.isNotEmpty(privateKeyPath)) {
                    if (StringUtils.isNotEmpty(passphrase)) {
                        defaultJSch.addIdentity(privateKeyPath, passphrase);
                    } else {
                        defaultJSch.addIdentity(privateKeyPath);
                    }
                }

                return defaultJSch;
            }

            @Override
            protected void configure(OpenSshConfig.Host host, Session session) {
                // Do nothing
            }

        };
    }

}
