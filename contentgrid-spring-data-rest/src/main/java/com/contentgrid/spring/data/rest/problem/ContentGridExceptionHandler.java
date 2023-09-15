package com.contentgrid.spring.data.rest.problem;

import com.contentgrid.spring.data.rest.problem.ext.ConstraintViolationProblemProperties;
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
    private final ProblemTypeUrlFactory problemTypeUrlFactory;

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
        return Problem.create()
                .withStatus(HttpStatus.CONFLICT)
                .withType(problemTypeUrlFactory.resolve(type))
                .withTitle(messageSourceAccessor.getMessage(type.withArguments(exception.getConstraintName())))
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
                        messageSourceAccessor.getMessage(error),
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
        var propertiesBuilder = ConstraintViolationProblemProperties.builder();

        if (exception.getPath().isEmpty()) {
            propertiesBuilder.global(
                    problemTypeUrlFactory.resolve(ProblemType.INVALID_REQUEST_BODY_TYPE),
                    messageSourceAccessor.getMessage(ProblemType.INVALID_REQUEST_BODY_TYPE)
            );
        } else {
            var jsonPath = exception.getPath().stream().map(Reference::getFieldName).collect(Collectors.joining("."));
            propertiesBuilder.field(
                    problemTypeUrlFactory.resolve(ProblemType.INVALID_REQUEST_BODY_TYPE),
                    messageSourceAccessor.getMessage(ProblemType.INVALID_REQUEST_BODY_TYPE),
                    jsonPath
            );
        }

        return Problem.create()
                .withStatus(HttpStatus.BAD_REQUEST)
                .withType(problemTypeUrlFactory.resolve(ProblemType.VALIDATION_CONSTRAINT_VIOLATION))
                .withTitle(messageSourceAccessor.getMessage(ProblemType.VALIDATION_CONSTRAINT_VIOLATION))
                .withProperties(propertiesBuilder.build());
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


    private static String formatJacksonError(JsonProcessingException exception) {
        var message = Objects.requireNonNullElse(exception.getOriginalMessage(), "No message");
        var location = exception.getLocation();
        if (location == null) {
            return message;
        }

        return message + " at " + location.offsetDescription();
    }

}
