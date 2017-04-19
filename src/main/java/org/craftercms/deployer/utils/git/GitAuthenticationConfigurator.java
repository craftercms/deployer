package org.craftercms.deployer.utils.git;

import org.eclipse.jgit.api.TransportCommand;

/**
 * Created by alfonso on 4/19/17.
 */
public interface GitAuthenticationConfigurator {

    void configureAuthentication(TransportCommand command);

}
