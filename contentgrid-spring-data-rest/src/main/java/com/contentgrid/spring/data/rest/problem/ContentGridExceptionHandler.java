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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor
public class ContentGridExceptionHandler {

    @NonNull
    private final ProblemFactory problemFactory;

    @NonNull
    private final MessageSourceAccessor messageSourceAccessor;

    @NonNull
    private final JsonPropertyPathConverter jsonPropertyPathConverter;

    @NonNull
    private final ResponseEntityFactory responseEntityFactory;

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<Problem> handleConstraintViolation(ConstraintViolationException exception) {
        var problem = switch (exception.getSQLState()) {
            case "23505", "23P01" -> problemFactory.createProblem(ProblemType.INPUT_DUPLICATE_VALUE, exception.getConstraintName())
                    .withStatus(HttpStatus.CONFLICT);
            default -> problemFactory.createProblem(ProblemType.CONSTRAINT_VIOLATION, exception.getConstraintName())
                    .withStatus(HttpStatus.BAD_REQUEST);
        };
        return responseEntityFactory.createResponse(problem);
    }

    @ExceptionHandler
    ResponseEntity<Problem> handleRepositoryConstraintViolationException(RepositoryConstraintViolationException ex) {
        var problem = problemFactory.createProblem(ProblemType.INPUT_VALIDATION, ex.getErrors().getErrorCount())
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

            return responseEntityFactory.createResponse(problem.withProperties(propertiesBuilder.build()));
        }

        return responseEntityFactory.createResponse(problem);
    }

    @ExceptionHandler
    ResponseEntity<Problem> handleMappingException(JsonMappingException exception) {
        var problem = problemFactory.createProblem(ProblemType.INVALID_REQUEST_BODY_TYPE)
                .withStatus(HttpStatus.BAD_REQUEST);

        if (exception.getPath().isEmpty()) {
            return responseEntityFactory.createResponse(problem);
        } else {
            var jsonPath = exception.getPath().stream().map(Reference::getFieldName).collect(Collectors.joining("."));
            return responseEntityFactory.createResponse(
                    problem.withProperties(new FieldViolationProblemProperties(jsonPath))
            );
        }
    }

    @ExceptionHandler
    ResponseEntity<Problem> handleJsonParseException(JsonParseException exception) {
        return responseEntityFactory.createResponse(
                problemFactory.createProblem(ProblemType.INVALID_REQUEST_BODY_JSON)
                .withStatus(HttpStatus.BAD_REQUEST)
                .withDetail(formatJacksonError(exception))
        );
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
