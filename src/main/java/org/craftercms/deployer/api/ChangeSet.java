/*
 * Copyright (C) 2007-2016 Crafter Software Corporation.
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
package org.craftercms.deployer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Created by alfonsovasquez on 30/11/16.
 */
public class ChangeSet {

    protected List<String> createdFiles;
    protected List<String> updatedFiles;
    protected List<String> deletedFiles;

    public ChangeSet(List<String> createdFiles, List<String> updatedFiles, List<String> deletedFiles) {
        this.createdFiles = createdFiles;
        this.updatedFiles = updatedFiles;
        this.deletedFiles = deletedFiles;
    }

    public List<String> getCreatedFiles() {
        return Collections.unmodifiableList(createdFiles);
    }

    public List<String> getUpdatedFiles() {
        return Collections.unmodifiableList(updatedFiles);
    }

    public List<String> getDeletedFiles() {
        return Collections.unmodifiableList(deletedFiles);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(createdFiles) &&
               CollectionUtils.isEmpty(updatedFiles) &&
               CollectionUtils.isEmpty(deletedFiles);
    }

}
