package com.contentgrid.spring.data.rest.problem;

import internal.org.springframework.content.rest.controllers.StoreRestExceptionHandler;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.versions.LockOwnerException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(basePackageClasses = StoreRestExceptionHandler.class)
@ResponseBody
@RequiredArgsConstructor
public class SpringContentRestExceptionHandler {
    @ExceptionHandler({ LockOwnerException.class,
            OptimisticLockException.class,
            OptimisticLockingFailureException.class,
            PessimisticLockException.class,
            PessimisticLockingFailureException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    Problem handleConflict(Exception e) {
        return Problem.create()
                .withStatus(HttpStatus.CONFLICT)
                .withTitle(e.getMessage());
    }

}
