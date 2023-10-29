/*
 * Copyright (C) 2007-2023 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.utils.config.yaml;

import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link YAMLConfiguration} extension that will preserve
 * the order of the keys in the YAML file.
 */
public class KeyOrderedYAMLConfiguration extends YAMLConfiguration {

    @Override
    protected Map<String, Object> constructMap(ImmutableNode node) {
        // Use a LinkedHashMap to preserve the order of the keys for each level
        final Map<String, Object> map = new LinkedHashMap<>(node.getChildren().size());
        node.forEach(cNode -> addEntry(map, cNode.getNodeName(), cNode.getChildren().isEmpty() ? cNode.getValue() : constructMap(cNode)));
        return map;
    }

    /**
     * This method is copied from {@link org.apache.commons.configuration2.AbstractYAMLBasedConfiguration},
     * since it has private access.
     * Adds a key value pair to a map, taking list structures into account. If a key is added which is already present in
     * the map, this method ensures that a list is created.
     *
     * @param map   the map
     * @param key   the key
     * @param value the value
     */
    private static void addEntry(final Map<String, Object> map, final String key, final Object value) {
        final Object oldValue = map.get(key);
        if (oldValue == null) {
            map.put(key, value);
        } else if (oldValue instanceof Collection) {
            // safe case because the collection was created by ourselves
            @SuppressWarnings("unchecked") final Collection<Object> values = (Collection<Object>) oldValue;
            values.add(value);
        } else {
            final Collection<Object> values = new ArrayList<>();
            values.add(oldValue);
            values.add(value);
            map.put(key, values);
        }
    }
}
