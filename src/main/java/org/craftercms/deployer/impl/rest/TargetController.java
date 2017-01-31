package org.craftercms.deployer.impl.rest;

import java.util.List;
import java.util.Map;

import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.utils.RestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static org.craftercms.deployer.impl.rest.RestConstants.TARGET_ID_PARAM_NAME;
import static org.craftercms.deployer.impl.rest.RestConstants.TARGET_ID_PATH_VAR_NAME;

/**
 * Created by alfonsovasquez on 12/29/16.
 */
@RestController
@RequestMapping(TargetController.BASE_URL)
public class TargetController {

    public static final String BASE_URL = "/api/1/target";
    public static final String CREATE_TARGET_URL = "/create";
    public static final String GET_TARGET_URL = "/get/{" + TARGET_ID_PATH_VAR_NAME + "}";
    public static final String GET_ALL_TARGETS_URL = "/get_all";
    public static final String DELETE_TARGET_URL = "/delete/{" + TARGET_ID_PATH_VAR_NAME + "}";
    public static final String DEPLOY_TARGET_URL = "/deploy/{" + RestConstants.TARGET_ID_PATH_VAR_NAME + "}";
    public static final String DEPLOY_ALL_TARGETS_URL = "/deploy_all";

    public static final String REPLACE_PARAM_NAME = "replace";
    public static final String TEMPLATE_NAME_PARAM_NAME = "template_name";

    protected TargetService targetService;
    protected DeploymentService deploymentService;

    @Autowired
    public TargetController(TargetService targetService, DeploymentService deploymentService) {
        this.targetService = targetService;
        this.deploymentService = deploymentService;
    }

    @RequestMapping(value = CREATE_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> createTarget(@RequestBody Map<String, Object> parameters) throws DeployerException {
        String id = RestUtils.getRequiredStringParam(parameters, TARGET_ID_PARAM_NAME);
        boolean replace = RestUtils.getBooleanParam(parameters, REPLACE_PARAM_NAME);
        String templateName = RestUtils.getStringParam(parameters, TEMPLATE_NAME_PARAM_NAME);

        parameters.keySet().removeIf(key -> key.equals(REPLACE_PARAM_NAME) || key.equals(TEMPLATE_NAME_PARAM_NAME));

        targetService.createTarget(id, replace, templateName, parameters);

        return new ResponseEntity<>(RestUtils.createOkMessageResponse(),
                                    RestUtils.setLocationHeader(new HttpHeaders(), BASE_URL + GET_TARGET_URL, id),
                                    HttpStatus.CREATED);
    }

    @RequestMapping(value = GET_TARGET_URL, method = RequestMethod.GET)
    public ResponseEntity<Target> getTarget(@PathVariable(TARGET_ID_PATH_VAR_NAME) String id) throws DeployerException {
        Target target = targetService.getTarget(id);

        return new ResponseEntity<>(target,
                                    RestUtils.setLocationHeader(new HttpHeaders(), BASE_URL + GET_TARGET_URL, id),
                                    HttpStatus.OK);
    }

    @RequestMapping(value = GET_ALL_TARGETS_URL, method = RequestMethod.GET)
    public ResponseEntity<List<Target>> getAllTargets() throws DeployerException {
        List<Target> targets = targetService.getAllTargets();

        return new ResponseEntity<>(targets,
                                    RestUtils.setLocationHeader(new HttpHeaders(), BASE_URL + GET_ALL_TARGETS_URL),
                                    HttpStatus.OK);
    }

    @RequestMapping(value = DELETE_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Void> deleteTarget(@PathVariable(TARGET_ID_PATH_VAR_NAME) String id) throws DeployerException {
        targetService.deleteTarget(id);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = DEPLOY_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Deployment> deployTarget(@PathVariable(TARGET_ID_PATH_VAR_NAME) String id) throws DeployerException {
        Deployment deployment = deploymentService.deployTarget(id);

        return new ResponseEntity<>(deployment, new HttpHeaders(), HttpStatus.OK);
    }

    @RequestMapping(value = DEPLOY_ALL_TARGETS_URL, method = RequestMethod.POST)
    public ResponseEntity<List<Deployment>> deployAllTargets() throws DeployerException {
        List<Deployment> deployments = deploymentService.deployAllTargets();

        return new ResponseEntity<>(deployments, new HttpHeaders(), HttpStatus.OK);
    }

}
