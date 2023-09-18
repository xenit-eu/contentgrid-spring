package com.contentgrid.spring.data.rest.problem;

import java.lang.reflect.InvocationTargetException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Overwrite the default spring-data-rest exception handlers to use problem details
 * @see RepositoryRestExceptionHandler
 */
@ControllerAdvice(basePackageClasses = RepositoryRestExceptionHandler.class)
@RequiredArgsConstructor
public class SpringDataRestRepositoryExceptionHandler {

    private final ResponseEntityFactory responseEntityFactory;

    @ExceptionHandler
    ResponseEntity<Problem> handleNotFound(ResourceNotFoundException ex) {
        return responseEntityFactory.createResponse(Problem.create()
                .withStatus(HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler
    ResponseEntity<Problem> handleNotReadable(HttpMessageNotReadableException ex) {
        return responseEntityFactory.createResponse(
                Problem.create()
                        .withStatus(HttpStatus.BAD_REQUEST)
                        .withTitle(ex.getMessage())
        );
    }

    @ExceptionHandler({ InvocationTargetException.class, IllegalArgumentException.class, ClassCastException.class,
            ConversionFailedException.class, NullPointerException.class })
    ResponseEntity<Problem> handleMiscFailures(Exception ex) {
        return responseEntityFactory.createResponse(
                Problem.create()
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .withTitle(ex.getMessage())
        );
    }

    @ExceptionHandler
    ResponseEntity<Problem> handleRepositoryConstraintViolationException(RepositoryConstraintViolationException ex) {
        return responseEntityFactory.createResponse(
                Problem.create()
                        .withStatus(HttpStatus.BAD_REQUEST)
                        .withTitle(ex.getMessage())
        );
    }

    @ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
    ResponseEntity<Problem> handleConflict(Exception ex) {
        return responseEntityFactory.createResponse(
                Problem.create()
                        .withStatus(HttpStatus.CONFLICT)
                        .withTitle(ex.getMessage())
        );
    }

}
