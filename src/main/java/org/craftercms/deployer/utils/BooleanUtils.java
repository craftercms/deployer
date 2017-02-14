/*
 * Copyright (C) 2007-2017 Crafter Software Corporation.
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
 * Created by alfonsovasquez on 2/13/17.
 */
public class BooleanUtils extends org.apache.commons.lang3.BooleanUtils {

    public static Boolean toBooleanObject(Object obj) {
        if (obj != null) {
            if (obj instanceof Boolean) {
                return (Boolean)obj;
            } else {
                return toBooleanObject(obj.toString());
            }
        } else {
            return null;
        }
    }

}
