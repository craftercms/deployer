package org.craftercms.deployer.utils.git;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * {@link GitAuthenticationConfigurator} that uses basic username/password authentication.
 *
 * @author avasquez
 */
public class BasicUsernamePasswordAuthConfigurator implements GitAuthenticationConfigurator {

    private String username;
    private String password;

    public BasicUsernamePasswordAuthConfigurator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void configureAuthentication(TransportCommand command) {
        command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
    }

}
