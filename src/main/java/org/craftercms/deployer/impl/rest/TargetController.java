package org.craftercms.deployer.impl.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.craftercms.commons.http.HttpUtils;
import org.craftercms.deployer.api.Target;
import org.craftercms.deployer.api.TargetManager;
import org.craftercms.deployer.api.exceptions.DeploymentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by alfonsovasquez on 12/29/16.
 */
@RestController
@RequestMapping("/api/1/target")
public class TargetController {

    public static final String CREATE_PARAM = "create";
    public static final String TEMPLATE_NAME_PARAM = "templateName";

    public static final String DELETE_SUCCESS_MESSAGE = "Successfully deleted target '%s'";
    public static final String DELETE_FAILURE_MESSAGE = "Delete of target '%s' failed";

    protected TargetManager targetManager;

    @Autowired
    public TargetController(TargetManager targetManager) {
        this.targetManager = targetManager;
    }

    @RequestMapping("/list/all")
    public List<Target> listAll() throws DeploymentException {
        return targetManager.getAllTargets();
    }

    @RequestMapping("/{" + RestConstants.TARGET_ID_PATH_VAR + "}")
    public Target getTarget(@PathVariable String targetId,
                            @RequestParam(value = CREATE_PARAM, required = false) boolean create,
                            @RequestParam(value = TEMPLATE_NAME_PARAM, required = false) String templateName,
                            HttpServletRequest request) throws DeploymentException {
        Map<String, Object> params = HttpUtils.createRequestParamsMap(request);
        params.remove(CREATE_PARAM);
        params.remove(TEMPLATE_NAME_PARAM);

        return targetManager.getTarget(targetId, create, templateName, params);
    }

    @RequestMapping(value = "/{" + RestConstants.TARGET_ID_PATH_VAR + "}/delete", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> deleteTarget(@PathVariable String targetId) throws DeploymentException {
        Map<String, String> body = new HashMap<>(1);
        HttpStatus status;

        if (targetManager.deleteTarget(targetId)) {
            status = HttpStatus.OK;
            body.put(RestConstants.MESSAGE_PROPERTY_NAME, String.format(DELETE_SUCCESS_MESSAGE, targetId));
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            body.put(RestConstants.MESSAGE_PROPERTY_NAME, String.format(DELETE_FAILURE_MESSAGE, targetId));
        }

        return new ResponseEntity<>(body, status);
    }

}
