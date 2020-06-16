/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as published by
 * the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.craftercms.deployer.impl.events;

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.events.DeploymentEventsStore;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.springframework.beans.factory.InitializingBean;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static java.lang.String.format;

/**
 * Implementation of {@link DeploymentEventsStore} that uses a file to store the events.
 *
 * @author joseross
 * @since 3.1.8
 */
public class FileBasedDeploymentEventsStore implements DeploymentEventsStore<Properties, Path>, InitializingBean {

    /**
     * The folder where all files are stored locally
     */
    protected String folderPath;

    /**
     * The pattern used to generate the name of the files
     */
    protected String filePattern;

    public FileBasedDeploymentEventsStore(String folderPath, String filePattern) {
        this.folderPath = folderPath;
        this.filePattern = filePattern;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Files.createDirectories(Paths.get(folderPath));
    }

    protected Path getPath(Target target) {
        return Paths.get(folderPath, format(filePattern, target.getId()));
    }

    @Override
    public Properties loadDeploymentEvents(Target target) throws DeployerException {
        Properties deploymentEvents = new Properties();
        Path deploymentEventsPath = getPath(target);
        if (Files.exists(deploymentEventsPath)) {
            try (Reader reader = Files.newBufferedReader(deploymentEventsPath, StandardCharsets.UTF_8)) {
                deploymentEvents.load(reader);
            } catch (IOException e) {
                throw new DeployerException("Error reading loading events file @ " + deploymentEventsPath, e);
            }
        }

        return deploymentEvents;
    }

    @Override
    public void saveDeploymentEvents(Target target, Properties deploymentEvents) throws DeployerException {
        Path deploymentEventsPath = getPath(target);
        try (Writer writer = Files.newBufferedWriter(deploymentEventsPath, StandardCharsets.UTF_8)) {
            deploymentEvents.store(writer, null);
        } catch (IOException e) {
            throw new DeployerException("Error saving deployment events file @ " + deploymentEventsPath, e);
        }
    }

    @Override
    public Path getSource(Target target) {
        return getPath(target);
    }

}
