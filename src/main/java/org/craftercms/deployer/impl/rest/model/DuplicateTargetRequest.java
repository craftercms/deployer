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
package org.craftercms.deployer.impl.rest.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidateNoTagsParam;
import org.craftercms.commons.validation.annotations.param.ValidateSecurePathParam;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.SITE_ID;

/**
 * Holds the parameters to duplicate a Target
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DuplicateTargetRequest {
    @NotEmpty
    @Size(max = 50)
    @EsapiValidatedParam(type = SITE_ID)
    private String sourceSiteName;
    @NotEmpty
    @Size(max = 50)
    @EsapiValidatedParam(type = SITE_ID)
    private String siteName;
    @NotEmpty
    @Size(max = 50)
    @ValidateNoTagsParam
    @ValidateSecurePathParam
    @EsapiValidatedParam(type = SITE_ID, message = "Value is not a valid environment name")
    private String env;

    public String getSourceSiteName() {
        return sourceSiteName;
    }

    public void setSourceSiteName(String sourceSiteName) {
        this.sourceSiteName = sourceSiteName;
    }

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

}
