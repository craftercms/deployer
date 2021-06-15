/*
 * Copyright (C) 2007-2021 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.api.exceptions;

import static java.lang.String.format;

/**
 * Exception thrown when an unsupported search engine is requested for a new target
 *
 * @author joseross
 * @since 4.0.0
 */
public class UnsupportedSearchEngineException extends DeployerException {

    public static final UnsupportedSearchEngineException CRAFTER_SEARCH =
            new UnsupportedSearchEngineException("CrafterSearch", "Elasticsearch",
            "https://docs.craftercms.org/en/4.0/developers/cook-books/how-tos/migrate-site-to-elasticsearch.html");

    protected UnsupportedSearchEngineException(String unsupportedSearchEngine, String newSearchEngine, String docUrl) {
        super(format("Unsupported search engine %s, please update your site to use %s. For more information see %s",
                        unsupportedSearchEngine, newSearchEngine, docUrl));
    }

}
