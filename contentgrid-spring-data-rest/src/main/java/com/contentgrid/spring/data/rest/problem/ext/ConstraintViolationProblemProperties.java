package com.contentgrid.spring.data.rest.problem.ext;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.hateoas.mediatype.problem.Problem;

/**
 * API representation for a constraint violation problem details
 * <p>
 * This is part of the public API of the application, don't make backwards incompatible changes
 */
@Value
@Builder
public class ConstraintViolationProblemProperties {

    @Singular
    @JsonProperty("errors")
    List<Problem> errors;

    public static class ConstraintViolationProblemPropertiesBuilder {

        public ConstraintViolationProblemPropertiesBuilder global(URI type, String message) {
            return error(Problem.create()
                    .withType(type)
                    .withTitle(message)
            );
        }

        public ConstraintViolationProblemPropertiesBuilder field(URI type, String message, String property) {
            return error(Problem.create()
                    .withType(type)
                    .withTitle(message)
                    .withProperties(new FieldViolationProblemProperties(property))
            );
        }

        public ConstraintViolationProblemPropertiesBuilder field(URI type, String message, String property,
                Object invalidValue) {
            return error(Problem.create()
                    .withType(type)
                    .withTitle(message)
                    .withProperties(new InvalidValueFieldViolationProblemProperties(property, invalidValue))
            );
        }
    }

    @Value
    @NonFinal
    private static class FieldViolationProblemProperties {

        String property;
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class InvalidValueFieldViolationProblemProperties extends FieldViolationProblemProperties {

        public InvalidValueFieldViolationProblemProperties(String property, Object invalidValue) {
            super(property);
            this.invalidValue = invalidValue;
        }

        @JsonProperty("invalid_value")
        Object invalidValue;
    }
}
