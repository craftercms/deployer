package org.craftercms.deployer.utils.git;

import org.eclipse.jgit.api.TransportCommand;

/**
 * Utility class that configures a Git connection based on an authentication strategy.
 *
 * @author avasquez
 */
public interface GitAuthenticationConfigurator {

    /**
     * Configures the authentication of the given {@link TransportCommand} based on a specific authentication strategy,
     * like HTTP basic authentication, SSH username/password authentication and SSH RSA key pair authentication.
     *
     * @param command the command to configure
     */
    void configureAuthentication(TransportCommand command);

}
