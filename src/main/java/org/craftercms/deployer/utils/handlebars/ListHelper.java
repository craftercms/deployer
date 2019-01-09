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
package org.craftercms.deployer.utils.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;

import java.io.IOException;
import java.lang.reflect.Array;

/**
 * Handlebars helper class that provides conditional list iteration, which means that it will only iterate if it's an {@code Iterable}
 * or an array, and will do normal Handlebars processing otherwise.
 *
 * @author avasquez
 */
public class ListHelper implements Helper<Object> {

    public static final String NAME = "list";
    public static final ListHelper INSTANCE = new ListHelper();

    @Override
    public Object apply(Object context, Options options) throws IOException {
        if (context instanceof Iterable) {
            StringBuilder ret = new StringBuilder();
            Iterable iterable = (Iterable)context;

            for (Object elem : iterable) {
                ret.append(options.fn(elem));
            }

            return ret.toString();
        } else if (context.getClass().isArray()) {
            StringBuilder ret = new StringBuilder();
            int length = Array.getLength(context);

            for (int i = 0; i < length; i++) {
                ret.append(options.fn(Array.get(context, i)));
            }

            return ret.toString();
        } else {
            return options.fn(context);
        }
    }

}
