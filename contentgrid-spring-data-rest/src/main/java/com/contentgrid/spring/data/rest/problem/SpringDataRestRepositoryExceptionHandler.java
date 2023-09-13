package com.contentgrid.spring.data.rest.problem;

import java.lang.reflect.InvocationTargetException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.data.rest.webmvc.RepositoryRestExceptionHandler;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Overwrite the default spring-data-rest exception handlers to use problem details
 * @see RepositoryRestExceptionHandler
 */
@ControllerAdvice(basePackageClasses = RepositoryRestExceptionHandler.class)
@ResponseBody
@RequiredArgsConstructor
public class SpringDataRestRepositoryExceptionHandler {

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Problem handleNotFound(ResourceNotFoundException ex) {
        return Problem.create()
                .withStatus(HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleNotReadable(HttpMessageNotReadableException ex) {
        return Problem.create()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withTitle(ex.getMessage());
    }

    @ExceptionHandler({ InvocationTargetException.class, IllegalArgumentException.class, ClassCastException.class,
            ConversionFailedException.class, NullPointerException.class })
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Problem handleMiscFailures(Exception ex) {
        return Problem.create()
                .withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .withTitle(ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleRepositoryConstraintViolationException(RepositoryConstraintViolationException ex) {
        return Problem.create()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withTitle(ex.getMessage());
    }

    @ExceptionHandler({ OptimisticLockingFailureException.class, DataIntegrityViolationException.class })
    @ResponseStatus(HttpStatus.CONFLICT)
    Problem handleConflict(Exception ex) {
        return Problem.create()
                .withStatus(HttpStatus.CONFLICT)
                .withTitle(ex.getMessage());
    }

}
