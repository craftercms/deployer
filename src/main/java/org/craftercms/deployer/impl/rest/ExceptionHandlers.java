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

import org.craftercms.commons.rest.BaseRestExceptionHandlers;
import org.craftercms.commons.rest.RestServiceUtils;
import org.craftercms.commons.validation.rest.ValidationAwareRestExceptionHandlers;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

/**
 * Extension of {@link BaseRestExceptionHandlers} that provides exception handlers for specific Crafter Deployer exceptions.
 *
 * @author avasquez
 */
@ControllerAdvice
public class ExceptionHandlers extends ValidationAwareRestExceptionHandlers {

    /**
     * Handles a {@link TargetNotFoundException} by returning a 404 NOT FOUND.
     *
     * @param ex        the exception
     * @param request   the current request
     *
     * @return the response entity, with the body and status
     */
    @ExceptionHandler(TargetNotFoundException.class)
    public ResponseEntity<Object> handleTargetNotFoundException(TargetNotFoundException ex, WebRequest request) {
        return handleExceptionInternal(ex, "Target not found", new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handles a {@link TargetAlreadyExistsException} by returning a 409 CONFLICT.
     *
     * @param ex        the exception
     * @param request   the current request
     *
     * @return the response entity, with the body and status
     */
    @ExceptionHandler(TargetAlreadyExistsException.class)
    public ResponseEntity<Object> handleTargetAlreadyExistsException(TargetAlreadyExistsException ex, WebRequest request) {
        HttpHeaders headers = RestServiceUtils.setLocationHeader(new HttpHeaders(),
                                                                 TargetController.BASE_URL + TargetController.GET_TARGET_URL,
                                                                 ex.getEnv(), ex.getSiteName());

        return handleExceptionInternal(ex, "Target already exists", headers, HttpStatus.CONFLICT, request);
    }

}
