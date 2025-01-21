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

import java.io.IOException;

/**
 * Store that holds a state for a target processor
 */
public interface ProcessorStateStore {

	/**
	 * Loads the stored state valuefor the specified target and processor.
	 *
	 * @param targetId      the target's ID
	 * @param processorName the processor's name
	 * @param suffix        the suffix to use for the state file name
	 * @return the stored state value, or null if not found
	 * @throws IOException if an error occurs while reading the file
	 */
	String load(String targetId, String processorName, String suffix) throws IOException;

	/**
	 * Stores the specified value for the target and processor.
	 *
	 * @param targetId      the target's ID
	 * @param processorName the processor's name
	 * @param suffix        the suffix to use for the state file name
	 * @throws IOException if an error occurs while writing the file
	 */
	void store(String targetId, String processorName, String suffix, String value) throws IOException;

	/**
	 * Delete the state files directory for the specified target.
	 *
	 * @param targetId the target's ID
	 */
	void delete(String targetId);
}
