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
package org.craftercms.deployer.impl.cluster;

import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.cluster.ClusterManagementService;
import org.craftercms.deployer.api.cluster.ClusterMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import static java.lang.String.format;
import static org.craftercms.deployer.api.cluster.ClusterMode.UNKNOWN;

/**
 * Implementation of {@link ClusterManagementService} that uses the Crafter Studio REST API.
 */
@Component("clusterManagementService")
public class ClusterManagementServiceImpl implements ClusterManagementService {

    @Autowired
    private final RestTemplate restTemplate;

    @Value("#{environment.acceptsProfiles(T(org.springframework.core.env.Profiles).of('crafter.studio.dbClusterPrimaryReplica'))}")
    private boolean clusterOn;

    public ClusterManagementServiceImpl(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ClusterMode getClusterMode(final Target target) {
        if (!clusterOn) {
            throw new IllegalStateException("Cluster is not enabled");
        }
        String studioUrl = target.getStudioUrl();
        String token = target.getStudioManagementToken();
        String clusterModeUrl = format("%s/api/2/cluster/mode?token=%s", studioUrl, token);

        ResponseEntity<GetClusterModeResult> responseEntity = restTemplate.getForEntity(clusterModeUrl, GetClusterModeResult.class);
        if (responseEntity.getStatusCode().isError()) {
            return UNKNOWN;
        }
        if (responseEntity.getBody() == null) {
            return UNKNOWN;
        }
        return responseEntity.getBody().getClusterMode();
    }

    @Override
    public boolean isClusterOn() {
        return clusterOn;
    }

    /**
     * Class to represent the response from the 'get cluster mode' Crafter Studio REST API.
     */
    static class GetClusterModeResult {
        private ClusterMode clusterMode;

        public ClusterMode getClusterMode() {
            return clusterMode;
        }

        public void setClusterMode(ClusterMode clusterMode) {
            this.clusterMode = clusterMode;
        }
    }
}
