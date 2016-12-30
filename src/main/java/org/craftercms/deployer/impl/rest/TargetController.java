package org.craftercms.deployer.impl.rest;

import java.util.List;

import org.craftercms.deployer.api.TargetContext;
import org.craftercms.deployer.api.TargetResolver;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by alfonsovasquez on 12/29/16.
 */
@RestController
@RequestMapping("/api/1/target")
public class TargetController {

    protected TargetResolver targetResolver;

    @Autowired
    public TargetController(TargetResolver targetResolver) {
        this.targetResolver = targetResolver;
    }

    @RequestMapping("/list/all")
    public List<TargetContext> listAll() throws DeploymentException {
        return targetResolver.resolveAll();
    }

}
