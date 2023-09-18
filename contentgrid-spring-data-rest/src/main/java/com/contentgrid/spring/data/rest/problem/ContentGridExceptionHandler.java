package com.contentgrid.spring.data.rest.problem;

import com.contentgrid.spring.data.rest.problem.ext.ConstraintViolationProblemProperties;
import com.contentgrid.spring.data.rest.problem.ext.ConstraintViolationProblemProperties.FieldViolationProblemProperties;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.data.rest.core.RepositoryConstraintViolationException;
import org.springframework.hateoas.mediatype.problem.Problem;
import org.springframework.http.HttpStatus;
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
    private final ProblemFactory problemFactory;

    @NonNull
    private final MessageSourceAccessor messageSourceAccessor;

    @NonNull
    private final JsonPropertyPathConverter jsonPropertyPathConverter;

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    Problem handleConstraintViolation(ConstraintViolationException exception) {
        var type = switch (exception.getSQLState()) {
            case "23505" -> ProblemType.UNIQUE_CONSTRAINT_VIOLATION;
            default -> ProblemType.CONSTRAINT_VIOLATION;
        };
        return problemFactory.createProblem(type, exception.getConstraintName())
                .withStatus(HttpStatus.CONFLICT);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleRepositoryConstraintViolationException(RepositoryConstraintViolationException ex) {
        var problem = problemFactory.createProblem(ProblemType.VALIDATION_CONSTRAINT_VIOLATION, ex.getErrors().getErrorCount())
                .withStatus(HttpStatus.BAD_REQUEST);

        if (ex.getErrors() instanceof BindingResult bindingResult) {
            var domainType = bindingResult.getTarget().getClass();
            var propertiesBuilder = ConstraintViolationProblemProperties.builder();

            bindingResult.getGlobalErrors().forEach(error -> {
                propertiesBuilder.global(Problem.create().withDetail(messageSourceAccessor.getMessage(error)));
            });

            bindingResult.getFieldErrors().forEach(error -> {
                propertiesBuilder.field(
                        Problem.create()
                                .withDetail(messageSourceAccessor.getMessage(error)),
                        jsonPropertyPathConverter.fromJavaPropertyPath(domainType, error.getField()),
                        error.getRejectedValue()
                );
            });

            return problem.withProperties(propertiesBuilder.build());
        }

        return problem;
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleMappingException(JsonMappingException exception) {
        var problem = problemFactory.createProblem(ProblemType.INVALID_REQUEST_BODY_TYPE)
                .withStatus(HttpStatus.BAD_REQUEST);

        if (exception.getPath().isEmpty()) {
            return problem;
        } else {
            var jsonPath = exception.getPath().stream().map(Reference::getFieldName).collect(Collectors.joining("."));
            return problem.withProperties(new FieldViolationProblemProperties(jsonPath));
        }
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Problem handleJsonParseException(JsonParseException exception) {
        return problemFactory.createProblem(ProblemType.INVALID_REQUEST_BODY_JSON)
                .withStatus(HttpStatus.BAD_REQUEST)
                .withDetail(formatJacksonError(exception));
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
