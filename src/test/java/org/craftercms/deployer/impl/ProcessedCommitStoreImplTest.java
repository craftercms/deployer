/*
 * Copyright (C) 2007-2019 Crafter Software Corporation. All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.lib.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ProcessedCommitsStore}.
 *
 * @author avasquez
 */
public class ProcessedCommitStoreImplTest {

    private static final ObjectId OBJECT_ID = ObjectId.fromString("ca33348b3f4a3dc6ed05acd25e349a30dfbe7108");

    private ProcessedCommitsStoreImpl processedCommitsStore;
    private File processedCommitsFolder;

    @Before
    public void setUp() throws Exception {
        processedCommitsFolder = createProcessedCommitsFolder();

        processedCommitsStore = new ProcessedCommitsStoreImpl();
        processedCommitsStore.setStoreFolder(processedCommitsFolder);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.forceDelete(processedCommitsFolder);
    }

    @Test
    public void testLoad() throws Exception {
        ObjectId objectId = processedCommitsStore.load("foobar-test");

        assertNotNull(objectId);
        assertEquals(OBJECT_ID, objectId);
    }

    @Test
    public void testStore() throws Exception {
        processedCommitsStore.store("barfoo-test", OBJECT_ID);

        File barfooTestCommitFile = new File(processedCommitsFolder, "barfoo-test.commit");

        assertTrue(barfooTestCommitFile.exists());
        assertEquals(OBJECT_ID, ObjectId.fromString(FileUtils.readFileToString(barfooTestCommitFile, "UTF-8").trim()));
    }

    @Test
    public void testDelete() throws Exception {
        processedCommitsStore.delete("foobar-test");

        File foobarTestFile = new File(processedCommitsFolder, "foobar-test.commit");

        assertFalse(foobarTestFile.exists());
    }

    private File createProcessedCommitsFolder() throws IOException {
        File tempProcessedCommitsFolder = Files.createTempDirectory("processed-commits").toFile();
        File classpathProcessedCommitsFolder = new ClassPathResource("processed-commits").getFile();

        FileUtils.copyDirectory(classpathProcessedCommitsFolder, tempProcessedCommitsFolder);

        return tempProcessedCommitsFolder;
    }

}
