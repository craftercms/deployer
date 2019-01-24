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
package org.craftercms.deployer.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.craftercms.search.batch.AbstractUpdateDetailProvider;

/**
 * The collection of created, updated and deleted files that have been changed in a deployment.
 *
 * @author avasquez
 */
public class ChangeSet extends AbstractUpdateDetailProvider {

    protected List<String> createdFiles;
    protected List<String> updatedFiles;
    protected List<String> deletedFiles;

    public ChangeSet() {
        this.createdFiles = new ArrayList<>();
        this.updatedFiles = new ArrayList<>();
        this.deletedFiles = new ArrayList<>();
    }

    public ChangeSet(List<String> createdFiles, List<String> updatedFiles, List<String> deletedFiles) {
        this.createdFiles = createdFiles;
        this.updatedFiles = updatedFiles;
        this.deletedFiles = deletedFiles;
    }

    /**
     * Returns the list of created files.
     */
    @JsonProperty("created_files")
    public List<String> getCreatedFiles() {
        return Collections.unmodifiableList(createdFiles);
    }

    /**
     * Returns the list of updated files.
     */
    @JsonProperty("updated_files")
    public List<String> getUpdatedFiles() {
        return Collections.unmodifiableList(updatedFiles);
    }

    /**
     * Returns the list of deleted files.
     */
    @JsonProperty("deleted_files")
    public List<String> getDeletedFiles() {
        return Collections.unmodifiableList(deletedFiles);
    }

    /**
     * Returns true if there are not created, updated or deleted files.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(createdFiles) &&
               CollectionUtils.isEmpty(updatedFiles) &&
               CollectionUtils.isEmpty(deletedFiles);
    }

}
