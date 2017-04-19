package org.craftercms.deployer.utils.git;

import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

/**
 * Created by alfonso on 4/19/17.
 */
public class UsernamePasswordAuthConfigurator implements GitAuthenticationConfigurator {

    private String username;
    private String password;

    public UsernamePasswordAuthConfigurator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public void configureAuthentication(TransportCommand command) {
        command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, password));
    }

}
