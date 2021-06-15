/*
 * Copyright (C) 2007-2020 Crafter Software Corporation. All Rights Reserved.
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
package org.craftercms.deployer.impl.rest;

import org.craftercms.commons.exceptions.InvalidManagementTokenException;
import org.craftercms.commons.rest.BaseRestExceptionHandlers;
import org.craftercms.commons.rest.RestServiceUtils;
import org.craftercms.commons.validation.rest.ValidationAwareRestExceptionHandlers;
import org.craftercms.deployer.api.exceptions.TargetAlreadyExistsException;
import org.craftercms.deployer.api.exceptions.TargetNotFoundException;
import org.craftercms.deployer.api.exceptions.UnsupportedSearchEngineException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
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
@Order(Ordered.HIGHEST_PRECEDENCE)
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

    /**
     * Handles a {@link UnsupportedSearchEngineException} by returning a 400 BAD REQUEST.
     *
     * @param ex        the exception
     * @param request   the current request
     *
     * @return the response entity, with the body and status
     */
    @ExceptionHandler(UnsupportedSearchEngineException.class)
    public ResponseEntity<Object> handleUnsupportedSearchEngineException(UnsupportedSearchEngineException ex,
                                                                         WebRequest request) {
        return handleExceptionInternal(ex, ex.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(InvalidManagementTokenException.class)
    public ResponseEntity<Object> handleInvalidManagementTokenException(InvalidManagementTokenException ex,
                                                                        WebRequest request) {
        return handleExceptionInternal(ex, "Invalid management token", new HttpHeaders(), HttpStatus.UNAUTHORIZED,
            request);
    }

}
