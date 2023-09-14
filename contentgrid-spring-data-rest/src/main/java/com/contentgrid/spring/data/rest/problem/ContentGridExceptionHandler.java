package com.contentgrid.spring.data.rest.problem;

import com.contentgrid.spring.data.rest.problem.ext.ConstraintViolationProblemProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolation;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
@ResponseBody
@RequiredArgsConstructor
public class ContentGridExceptionHandler {

    @NonNull
    private final ProblemTypeUrlFactory problemTypeUrlFactory;

    @NonNull
    private final MessageSourceAccessor messageSourceAccessor;

    @NonNull
    private final JsonPropertyPathConverter jsonPropertyPathConverter;

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Problem handleConstraintViolation(ConstraintViolationException exception) {
        return Problem.create()
                .withStatus(HttpStatus.CONFLICT)
                .withType(problemTypeUrlFactory.resolve(ProblemType.CONSTRAINT_VIOLATION))
                .withTitle(messageSourceAccessor.getMessage(
                        ProblemType.CONSTRAINT_VIOLATION.withArguments(exception.getConstraintName())))
                .withDetail(exception.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleRepositoryConstraintViolationException(RepositoryConstraintViolationException ex) {
        var problem = Problem.create()
                .withType(problemTypeUrlFactory.resolve(ProblemType.VALIDATION_CONSTRAINT_VIOLATION))
                .withStatus(HttpStatus.BAD_REQUEST)
                .withTitle(messageSourceAccessor.getMessage(ProblemType.VALIDATION_CONSTRAINT_VIOLATION));

        if (ex.getErrors() instanceof BindingResult bindingResult) {
            var domainType = bindingResult.getTarget().getClass();
            var propertiesBuilder = ConstraintViolationProblemProperties.builder();

            bindingResult.getGlobalErrors().forEach(error -> {
                propertiesBuilder.global(null, messageSourceAccessor.getMessage(error));
            });

            bindingResult.getFieldErrors().forEach(error -> {
                propertiesBuilder.field(
                        null,
                        jsonPropertyPathConverter.fromJavaPropertyPath(domainType, error.getField()),
                        messageSourceAccessor.getMessage(error),
                        error.getRejectedValue()
                );
            });

            return problem.withProperties(propertiesBuilder.build());
        }

        return problem;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleMismatchedInput(MismatchedInputException exception) {
        return Problem.create()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withType(problemTypeUrlFactory.resolve(ProblemType.VALIDATION_CONSTRAINT_VIOLATION))
                .withTitle(messageSourceAccessor.getMessage(ProblemType.VALIDATION_CONSTRAINT_VIOLATION))
                .withProperties(
                        ConstraintViolationProblemProperties.builder()
                                .global(
                                        problemTypeUrlFactory.resolve(ProblemType.INVALID_REQUEST_BODY_TYPE),
                                        messageSourceAccessor.getMessage(ProblemType.INVALID_REQUEST_BODY_TYPE)
                                )
                                .build()
                );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleJsonParseException(JsonParseException exception) {
        return Problem.create()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withType(problemTypeUrlFactory.resolve(ProblemType.INVALID_REQUEST_BODY_JSON))
                .withTitle(messageSourceAccessor.getMessage(ProblemType.INVALID_REQUEST_BODY_JSON))
                .withDetail(formatJacksonError(exception));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleNotReadable(HttpMessageNotReadableException exception) {
        return Problem.create()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withType(problemTypeUrlFactory.resolve(ProblemType.INVALID_REQUEST_BODY))
                .withTitle(messageSourceAccessor.getMessage(ProblemType.INVALID_REQUEST_BODY))
                .withDetail(exception.getMessage());
    }

    private static String formatJacksonError(JsonProcessingException exception) {
        var message = Objects.requireNonNullElse(exception.getOriginalMessage(), "No message");
        var location = exception.getLocation();
        if (location == null) {
            return message;
        }

        return message + " at " + location.offsetDescription();
    }

}
