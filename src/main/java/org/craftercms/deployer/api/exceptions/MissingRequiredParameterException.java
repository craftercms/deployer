package org.craftercms.deployer.api.exceptions;

/**
 * Created by alfonsovasquez on 1/26/17.
 */
public class MissingRequiredParameterException extends DeployerException {

    protected String paramName;

    public MissingRequiredParameterException(String paramName) {
        super("Missing parameter '" + paramName + "'");

        this.paramName = paramName;
    }

    public String getParamName() {
        return paramName;
    }

}
