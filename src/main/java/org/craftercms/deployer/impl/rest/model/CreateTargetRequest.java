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

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import org.craftercms.commons.validation.annotations.param.EsapiValidatedParam;
import org.craftercms.commons.validation.annotations.param.ValidateNoTagsParam;
import org.craftercms.commons.validation.annotations.param.ValidateSecurePathParam;
import org.craftercms.commons.validation.annotations.param.ValidateStringParam;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.craftercms.commons.validation.annotations.param.EsapiValidationType.*;

/**
 * Holds the parameters to create a Target
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CreateTargetRequest {
    @NotEmpty
    @EsapiValidatedParam(type = SITE_ID)
    private String siteName;
    @NotEmpty
    @ValidateNoTagsParam
    @ValidateSecurePathParam
    private String env;
    private boolean replace;
    @NotBlank
    @ValidateNoTagsParam
    private String templateName = "remote";
    @ValidateNoTagsParam
    private String repoUrl;
    @ValidateNoTagsParam
    @ValidateSecurePathParam
    private String repoBranch;
    @EsapiValidatedParam(type = USERNAME)
    private String repoUsername;
    @ValidateNoTagsParam
    @ValidateSecurePathParam
    private String sshPrivateKeyPath;
    @ValidateNoTagsParam
    private String engineUrl;
    private List<@NotBlank @EsapiValidatedParam(type = EMAIL) String> notificationAddresses;
    @ValidateStringParam(whitelistedPatterns = "CrafterSearch|Elasticsearch")
    private String searchEngine = "Elasticsearch";

    @JsonUnwrapped
    private final Map<String, Object> extraParams;

    public CreateTargetRequest() {
        this.extraParams = new HashMap<>();
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

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getRepoBranch() {
        return repoBranch;
    }

    public void setRepoBranch(String repoBranch) {
        this.repoBranch = repoBranch;
    }

    public String getRepoUsername() {
        return repoUsername;
    }

    public void setRepoUsername(String repoUsername) {
        this.repoUsername = repoUsername;
    }

    public String getSshPrivateKeyPath() {
        return sshPrivateKeyPath;
    }

    public void setSshPrivateKeyPath(String sshPrivateKeyPath) {
        this.sshPrivateKeyPath = sshPrivateKeyPath;
    }

    public String getEngineUrl() {
        return engineUrl;
    }

    public void setEngineUrl(String engineUrl) {
        this.engineUrl = engineUrl;
    }

    public List<String> getNotificationAddresses() {
        return notificationAddresses;
    }

    public void setNotificationAddresses(List<String> notificationAddresses) {
        this.notificationAddresses = notificationAddresses;
    }

    public String getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(String searchEngine) {
        this.searchEngine = searchEngine;
    }

    @JsonAnySetter
    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    @JsonAnySetter
    public void addExtraParam(String key, Object value) {
        this.extraParams.put(key, value);
    }
}
