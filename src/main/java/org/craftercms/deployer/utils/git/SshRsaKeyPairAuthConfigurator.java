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
 * {@link GitAuthenticationConfigurator} that configures the {@code TransportCommand} to use SSH with RSA key pair authentication.
 * The file path of the private key and it's passphrase can be provided, but are not necessary, specially when the private key has
 * already been loaded into the SSH agent.
 *
 * @author avasquez
 */
public class SshRsaKeyPairAuthConfigurator extends AbstractSshAuthConfigurator {

    protected String privateKeyPath;
    protected String passphrase;

    public void setPrivateKeyPath(String privateKeyPath) {
        this.privateKeyPath = privateKeyPath;
    }

    public void setPassphrase(String passphrase) {
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
