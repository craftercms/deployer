/*
 * Copyright (C) 2007-2025 Crafter Software Corporation. All Rights Reserved.
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

package org.craftercms.deployer.impl;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static org.craftercms.commons.config.ConfigUtils.DEFAULT_ENCODING;

/**
 * Default implementation of {@link ProcessorStateStore} that stores the data
 * in a file in a dedicated directory under deployer data.
 */
public class ProcessorStateStoreImpl implements ProcessorStateStore {

    private static final Logger logger = LoggerFactory.getLogger(ProcessorStateStoreImpl.class);
    private File storeFolder;

    @Override
    public String load(String targetId, String processorName, String suffix) throws IOException {
        File lastDateFile = getLastDateFile(targetId, processorName, suffix);
        if (!lastDateFile.exists()) {
            logger.info("State file does not exist '{}'", lastDateFile);
            return null;
        }
        return FileUtils.readFileToString(lastDateFile, "UTF-8").trim();
    }

    @Override
    public void store(String targetId, String processorName, String suffix, String value) throws IOException {
        FileUtils.write(getLastDateFile(targetId, processorName, suffix),
                value, DEFAULT_ENCODING, false);
    }

    @Override
    public void delete(String targetId) {
        File targetNotificationsFolder = new File(storeFolder, targetId);
        if (targetNotificationsFolder.exists()) {
            logger.info("Deleting processor state files directory for target '{}'", targetId);
            FileUtils.deleteQuietly(targetNotificationsFolder);
        }
    }

    /**
     * Returns the state file for the given processor.
     */
    private File getLastDateFile(String targetId, String processorName, String suffix) {
        String filename = "%s/%s-%s".formatted(targetId, processorName, suffix);
        return new File(storeFolder, filename);
    }

    public void setStoreFolder(File storeFolder) {
        this.storeFolder = storeFolder;
    }
}
