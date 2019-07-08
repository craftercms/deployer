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
package org.craftercms.deployer.impl.rest;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.craftercms.commons.rest.RestServiceUtils;
import org.craftercms.commons.rest.Result;
import org.craftercms.commons.validation.ErrorCodes;
import org.craftercms.commons.validation.ValidationException;
import org.craftercms.commons.validation.ValidationResult;
import org.craftercms.deployer.api.Deployment;
import org.craftercms.deployer.api.DeploymentService;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetService;
import org.craftercms.deployer.api.exceptions.DeployerException;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.TargetServiceException;
import org.craftercms.deployer.utils.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.craftercms.deployer.impl.rest.RestConstants.*;

/**
 * Main controller for target related operations.
 *
 * @author avasquez
 */
@RestController
@RequestMapping(TargetController.BASE_URL)
public class TargetController {

    public static final String BASE_URL                        = "/api/1/target";
    public static final String CREATE_TARGET_URL               = "/create";
    public static final String CREATE_TARGET_IF_NOT_EXISTS_URL = "/create_if_not_exists";
    public static final String GET_TARGET_URL                  = "/get/{" + ENV_PATH_VAR_NAME + "}/" +
                                                                 "{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String GET_ALL_TARGETS_URL             = "/get-all";
    public static final String DELETE_TARGET_URL               = "/delete/{" + ENV_PATH_VAR_NAME + "}/" +
                                                                 "{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String DELETE_IF_EXIST_TARGET_URL      = "/delete-if-exists/{" + ENV_PATH_VAR_NAME + "}/" +
                                                                 "{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String DEPLOY_TARGET_URL               = "/deploy/{" + ENV_PATH_VAR_NAME + "}/" +
                                                                 "{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String DEPLOY_ALL_TARGETS_URL          = "/deploy-all";
    public static final String GET_PENDING_DEPLOYMENTS_URL     = "/deployments/get-pending/{" + ENV_PATH_VAR_NAME + "}/" +
                                                                 "{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String GET_CURRENT_DEPLOYMENT_URL      = "/deployments/get-current/{" + ENV_PATH_VAR_NAME + "}/" +
                                                                 "{" + SITE_NAME_PATH_VAR_NAME + "}";
    public static final String GET_ALL_DEPLOYMENTS_URL         = "/deployments/get-all/{" + ENV_PATH_VAR_NAME + "}/" +
                                                                 "{" + SITE_NAME_PATH_VAR_NAME + "}";

    public static final String REPLACE_PARAM_NAME = "replace";
    public static final String TEMPLATE_NAME_PARAM_NAME = "template_name";
    public static final String SEARCH_ENGINE_PARAM_NAME = "search_engine";
    public static final String SEARCH_ENGINE_PARAM_VALUE = "CrafterSearch";

    protected TargetService targetService;
    protected DeploymentService deploymentService;

    @Autowired
    public TargetController(TargetService targetService, DeploymentService deploymentService) {
        this.targetService = targetService;
        this.deploymentService = deploymentService;
    }

    /**
     * Creates a Deployer {@link Target}.
     *
     * @param params the body of the request with the template parameters that will be used to create the target.
     *               The body must contain at least a {@code env} and {@code site_name} parameter. Other required
     *               parameters depend on the template used.
     *
     * @return the response entity 201 CREATED status
     *
     * @throws DeployerException   if an error ocurred during target creation
     * @throws ValidationException if a required parameter is missing
     */
    @RequestMapping(value = CREATE_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Result> createTarget(@RequestBody Map<String, Object> params) throws DeployerException,
                                                                                               ValidationException {
        return createTarget(params, false);
    }

    @RequestMapping(value = CREATE_TARGET_IF_NOT_EXISTS_URL, method = RequestMethod.POST)
    public ResponseEntity<Result> createTargetIfNotExists(
            @RequestBody Map<String, Object> params) throws DeployerException, ValidationException {
        return createTarget(params, true);
    }

    /**
     * Returns a {@link Target}.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the response entity with the target's properties and 200 OK status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = GET_TARGET_URL, method = RequestMethod.GET)
    public ResponseEntity<Target> getTarget(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                            @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName)
            throws DeployerException {
        Target target = targetService.getTarget(env, siteName);

        return new ResponseEntity<>(target,
                                    createResponseHeaders(BASE_URL + GET_TARGET_URL, env, siteName),
                                    HttpStatus.OK);
    }

    /**
     * Returns all current {@link Target}s
     *
     * @return the response entity with all the properties of the targets and 200 OK status
     *
     * @throws DeployerException if an error ocurred
     */
    @RequestMapping(value = GET_ALL_TARGETS_URL, method = RequestMethod.GET)
    public ResponseEntity<List<Target>> getAllTargets() throws DeployerException {
        List<Target> targets = targetService.getAllTargets();

        return new ResponseEntity<>(targets,
                                    createResponseHeaders(BASE_URL + GET_ALL_TARGETS_URL),
                                    HttpStatus.OK);
    }

    /**
     * Deletes the {@link Target} with the specified environment and site name.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the response entity with a 204 NO CONTENT status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = DELETE_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Void> deleteTarget(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                             @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName)
            throws DeployerException {
        targetService.deleteTarget(env, siteName);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Deletes the {@link Target} with the specified environment and site name, does nothing if the target is not found.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the response entity with a 204 NO CONTENT status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = DELETE_IF_EXIST_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Void> deleteTargetIfExists(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                                     @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName)
            throws DeployerException {

        try {
            targetService.deleteTarget(env, siteName);
        } catch (TargetNotFoundException e) {
            // do nothing
        }

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Deploys the {@link Target} with the specified environment and site name.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     * @param params    any additional parameters that can be used by the
     *                  {@link org.craftercms.deployer.api.DeploymentProcessor}s, for
     *                  example {@code reprocess_all_files}
     *
     * @return the response entity with a 200 OK status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = DEPLOY_TARGET_URL, method = RequestMethod.POST)
    public ResponseEntity<Result> deployTarget(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                               @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName,
                                               @RequestBody(required = false) Map<String, Object> params)
                            throws DeployerException {
        if (params == null) {
            params = new HashMap<>();
        }

        boolean waitTillDone = false;
        if (MapUtils.isNotEmpty(params)) {
            waitTillDone = BooleanUtils.toBoolean(params.remove(WAIT_TILL_DONE_PARAM_NAME));
        }

        deploymentService.deployTarget(env, siteName, waitTillDone, params);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.OK);
    }

    /**
     * Deploys all current {@link Target}s.
     *
     * @param params    any additional parameters that can be used by the
     *                  {@link org.craftercms.deployer.api.DeploymentProcessor}s, for
     *                  example {@code reprocess_all_files}
     *
     * @return the response entity with a 200 OK status
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = DEPLOY_ALL_TARGETS_URL, method = RequestMethod.POST)
    public ResponseEntity<Result> deployAllTargets(@RequestBody(required = false) Map<String, Object> params)
            throws DeployerException {
        if (params == null) {
            params = new HashMap<>();
        }

        boolean waitTillDone = false;
        if (MapUtils.isNotEmpty(params)) {
           waitTillDone = BooleanUtils.toBoolean(params.remove(WAIT_TILL_DONE_PARAM_NAME));
        }

        deploymentService.deployAllTargets(waitTillDone, params);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.OK);
    }

    /**
     * Gets the pending deployments for a target.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the pending deployments for the target
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = GET_PENDING_DEPLOYMENTS_URL, method = RequestMethod.GET)
    public ResponseEntity<Collection<Deployment>> getPendingDeployments(
            @PathVariable(ENV_PATH_VAR_NAME) String env,
            @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName) throws DeployerException {
        Target target = targetService.getTarget(env, siteName);
        Collection<Deployment> deployments = target.getPendingDeployments();

        return new ResponseEntity<>(deployments,
                                    createResponseHeaders(BASE_URL +GET_PENDING_DEPLOYMENTS_URL, env, siteName),
                                    HttpStatus.OK);
    }

    /**
     * Gets the current deployment for a target.
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the pending and current deployments for the target
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = GET_CURRENT_DEPLOYMENT_URL, method = RequestMethod.GET)
    public ResponseEntity<Deployment> getCurrentDeployment(@PathVariable(ENV_PATH_VAR_NAME) String env,
                                                           @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName)
            throws DeployerException {
        Target target = targetService.getTarget(env, siteName);
        Deployment deployment = target.getCurrentDeployment();

        return new ResponseEntity<>(deployment,
                                    createResponseHeaders(BASE_URL + GET_CURRENT_DEPLOYMENT_URL, env, siteName),
                                    HttpStatus.OK);
    }


    /**
     * Gets all deployments for a target (pending and current).
     *
     * @param env       the target's environment
     * @param siteName  the target's site name
     *
     * @return the pending and current deployments for the target
     *
     * @throws DeployerException if an error occurred
     */
    @RequestMapping(value = GET_ALL_DEPLOYMENTS_URL, method = RequestMethod.GET)
    public ResponseEntity<Collection<Deployment>> getAllDeployments(
            @PathVariable(ENV_PATH_VAR_NAME) String env,
            @PathVariable(SITE_NAME_PATH_VAR_NAME) String siteName) throws DeployerException {
        Target target = targetService.getTarget(env, siteName);
        Collection<Deployment> deployments = target.getAllDeployments();

        return new ResponseEntity<>(deployments,
                                    createResponseHeaders(BASE_URL + GET_ALL_DEPLOYMENTS_URL, env, siteName),
                                    HttpStatus.OK);
    }

    protected ResponseEntity<Result> createTarget(Map<String, Object> params, boolean createIfNotExists)
            throws ValidationException, TargetServiceException, TargetAlreadyExistsException {
        String env = "";
        String siteName = "";
        boolean replace = false;
        String templateName = "";
        boolean crafterSearchEnabled = false;
        Map<String, Object> templateParams = new HashMap<>();

        for (Map.Entry<String, Object> param : params.entrySet()) {
            switch (param.getKey()) {
                case ENV_PATH_VAR_NAME:
                    env = param.getValue().toString();
                    break;
                case SITE_NAME_PATH_VAR_NAME:
                    siteName = param.getValue().toString();
                    break;
                case REPLACE_PARAM_NAME:
                    replace = BooleanUtils.toBoolean(param.getValue());
                    break;
                case TEMPLATE_NAME_PARAM_NAME:
                    templateName = param.getValue().toString();
                    break;
                case SEARCH_ENGINE_PARAM_NAME:
                    crafterSearchEnabled = SEARCH_ENGINE_PARAM_VALUE.equals(param.getValue());
                    break;
                default:
                    templateParams.put(param.getKey(), param.getValue());
                    break;
            }
        }

        ValidationResult validationResult = new ValidationResult();
        if (StringUtils.isEmpty(env)) {
            validationResult.addError(ENV_PATH_VAR_NAME, ErrorCodes.FIELD_MISSING_ERROR_CODE);
        }
        if (StringUtils.isEmpty(siteName)) {
            validationResult.addError(SITE_NAME_PATH_VAR_NAME, ErrorCodes.FIELD_MISSING_ERROR_CODE);
        }

        if (validationResult.hasErrors()) {
            throw new ValidationException(validationResult);
        }

        if (createIfNotExists) {
            if (!targetService.targetExists(env, siteName)) {
                targetService.createTarget(env, siteName, false, templateName, crafterSearchEnabled, templateParams);
            }
        } else {
            targetService.createTarget(env, siteName, replace, templateName, crafterSearchEnabled, templateParams);
        }

        return new ResponseEntity<>(Result.OK,
                                    createResponseHeaders(BASE_URL + GET_TARGET_URL, env, siteName),
                                    HttpStatus.CREATED);
    }

    protected HttpHeaders createResponseHeaders(String locationUrlTemplate, Object... variables) {
        return RestServiceUtils.setLocationHeader(new HttpHeaders(), locationUrlTemplate, variables);
    }

}
