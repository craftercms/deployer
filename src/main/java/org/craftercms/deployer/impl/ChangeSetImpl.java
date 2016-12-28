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
package org.craftercms.deployer.impl;

import java.util.ArrayList;
import java.util.List;

import org.craftercms.deployer.api.ChangeSet;

/**
 * Created by alfonsovasquez on 5/12/16.
 */
public class ChangeSetImpl implements ChangeSet {

    protected List<String> createdFiles;
    protected List<String> updatedFiles;
    protected List<String> deletedFiles;

    public ChangeSetImpl() {
        createdFiles = new ArrayList<>();
        updatedFiles = new ArrayList<>();
        deletedFiles = new ArrayList<>();
    }

    public ChangeSetImpl(List<String> createdFiles, List<String> updatedFiles, List<String> deletedFiles) {
        this.createdFiles = createdFiles;
        this.updatedFiles = updatedFiles;
        this.deletedFiles = deletedFiles;
    }

    @Override
    public List<String> getCreatedFiles() {
        return createdFiles;
    }

    public void setCreatedFiles(List<String> createdFiles) {
        this.createdFiles = createdFiles;
    }

    @Override
    public List<String> getUpdatedFiles() {
        return updatedFiles;
    }

    public void setUpdatedFiles(List<String> updatedFiles) {
        this.updatedFiles = updatedFiles;
    }

    @Override
    public List<String> getDeletedFiles() {
        return deletedFiles;
    }

    public void setDeletedFiles(List<String> deletedFiles) {
        this.deletedFiles = deletedFiles;
    }

}
