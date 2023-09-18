package com.contentgrid.spring.data.rest.problem;

import com.contentgrid.spring.data.rest.problem.ProblemTypeUrlFactory.ProblemTypeResolvable;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;

public enum ProblemType implements ProblemTypeResolvable {
    VALIDATION_CONSTRAINT_VIOLATION("integrity", "validation-constraint-violation"),
    CONSTRAINT_VIOLATION("integrity", "constraint-violation"),
    UNIQUE_CONSTRAINT_VIOLATION("integrity", "constraint-violation", "unique"),

    INVALID_REQUEST_BODY("invalid-request-body"),
    INVALID_REQUEST_BODY_TYPE("invalid-request-body", "type"),
    INVALID_REQUEST_BODY_JSON("invalid-request-body", "json");

    ProblemType(String... params) {
        this.params = params;
    }

    private final static String CLASSNAME = ProblemType.class.getName();

    final String[] params;

    @Override
    public String[] getProblemHierarchy() {
        return params;
    }

    public MessageSourceResolvable forTitle() {
        return new ProblemDetailsMessageSourceResolvable(CLASSNAME+".title", this, new Object[0]);

    }

    public MessageSourceResolvable forDetails(Object... arguments) {
        return new ProblemDetailsMessageSourceResolvable(CLASSNAME+".detail", this, arguments);
    }

    @RequiredArgsConstructor
    private static class ProblemDetailsMessageSourceResolvable implements MessageSourceResolvable {
        private final String prefix;

        private final ProblemType problemType;

        private final Object[] arguments;

        @Override
        public String[] getCodes() {
            var paramsList = Arrays.asList(problemType.params);
            var codes = new String[problemType.params.length];

            for (int i = codes.length; i > 0; i--) {
                codes[codes.length - i] = prefix + "." + String.join(".", paramsList.subList(0, i));
            }
            return codes;
        }

        @Override
        public Object[] getArguments() {
            return arguments;
        }

        @Override
        public String getDefaultMessage() {
            return ProblemTypeMessageSource.getAccessor().getMessage(this);
        }
    }
}
