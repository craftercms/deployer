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
package org.craftercms.deployer.impl.upgrade.operations;

import org.apache.commons.io.IOUtils;
import org.craftercms.commons.crypto.TextEncryptor;
import org.craftercms.commons.upgrade.impl.operations.AbstractUpgradeOperation;
import org.craftercms.deployer.api.Target;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of {@link AbstractUpgradeOperation} that updates all encrypted values in the target configuration.
 *
 * @author joseross
 * @since 3.1.9
 */
public class EncryptionUpgradeOperation extends AbstractUpgradeOperation<Target> {

    protected static String DEFAULT_ENCRYPTED_PATTERN = "\\$\\{enc:([^}#]+)}";

    protected Pattern encryptedPattern;

    protected TextEncryptor textEncryptor;

    public EncryptionUpgradeOperation(TextEncryptor textEncryptor) {
        this.encryptedPattern = Pattern.compile(DEFAULT_ENCRYPTED_PATTERN);
        this.textEncryptor = textEncryptor;
    }

    @Override
    protected void doExecute(Target target) throws Exception {
        File file = target.getConfigurationFile();

        String content;
        try (Reader reader = new FileReader(file)) {
            content = IOUtils.toString(reader);
        }

        Matcher matcher = encryptedPattern.matcher(content);
        boolean updateFile = matcher.matches();
        // for each one
        while(matcher.find()) {
            String encryptedValue = matcher.group(1);
            // decrypt it
            String originalValue = textEncryptor.decrypt(encryptedValue);
            // encrypt it again
            String newValue = textEncryptor.encrypt(originalValue);
            // replace it
            content = content.replaceAll(encryptedValue, newValue);

            updateFile = true;
        }

        // update the file if needed
        if (updateFile) {
            try (Writer writer = new FileWriter(file)) {
                IOUtils.write(content, writer);
            }
        }
    }

}
