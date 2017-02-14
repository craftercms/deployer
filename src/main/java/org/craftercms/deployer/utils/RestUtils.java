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

import java.net.URI;
import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by alfonsovasquez on 1/27/17.
 */
public class RestUtils {

    public static final String MESSAGE_PROPERTY_NAME = "message";

    private RestUtils() {
    }

    public static Map<String, String> createMessageResponse(String message) {
        return Collections.singletonMap(MESSAGE_PROPERTY_NAME, message);
    }

    public static HttpHeaders setLocationHeader(HttpHeaders headers, String url, Object... variables) {
        URI location = UriComponentsBuilder.fromUriString(url).buildAndExpand(variables).toUri();
        headers.setLocation(location);

        return headers;
    }
    
}
