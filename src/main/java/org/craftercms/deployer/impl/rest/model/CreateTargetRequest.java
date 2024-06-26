/*
 * Copyright (C) 2007-2024 Crafter Software Corporation. All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidSiteId;
import org.craftercms.commons.validation.annotations.param.ValidateNoTagsParam;
import org.craftercms.commons.validation.annotations.param.ValidateSecurePathParam;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.SITE_ID;

/**
 * Holds the parameters to create a Target
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateTargetRequest {
    @NotEmpty
    @ValidSiteId(message = "Value is not a valid environment name")
    private String env;
    @NotEmpty
    @ValidSiteId
    private String siteName;

    public String getSiteName() {
        return siteName;
    }

    public void setSiteName(String siteName) {
        this.siteName = siteName;
    }

    @JsonUnwrapped
    private TargetTemplateParams targetTemplateParams;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public TargetTemplateParams getTargetTemplateParams() {
        return targetTemplateParams;
    }

    public void setTargetTemplateParams(TargetTemplateParams targetTemplateParams) {
        this.targetTemplateParams = targetTemplateParams;
    }

}
