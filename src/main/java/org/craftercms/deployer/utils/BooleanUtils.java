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
package org.craftercms.deployer.utils;

/**
 * Utility methods for booleans.
 *
 * @author avasquez
 */
public class BooleanUtils extends org.apache.commons.lang3.BooleanUtils {

    /**
     * Converts an object to boolean, according to the following logic:
     *
     * <ol>
     *     <li>If it's null, {@code false} is returned.</li>
     *     <li>If it's a Boolean, the same object is returned.</li>
     *     <li>If it's any other object, the {@code toString()} value is converted to boolean.</li>
     * </ol>
     *
     * @param obj
     * @return boolean value
     */
    public static boolean toBoolean(Object obj) {
        if (obj != null) {
            if (obj instanceof Boolean) {
                return (Boolean)obj;
            } else {
                return toBoolean(obj.toString());
            }
        } else {
            return false;
        }
    }

}
