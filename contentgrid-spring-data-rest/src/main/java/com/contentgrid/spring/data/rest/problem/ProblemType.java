package com.contentgrid.spring.data.rest.problem;

import com.contentgrid.spring.data.rest.problem.ProblemTypeUrlFactory.ProblemTypeResolvable;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;

public enum ProblemType implements MessageSourceResolvable, ProblemTypeResolvable {
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
    private String[] codes = null;

    @Override
    public String[] getProblemHierarchy() {
        return params;
    }

    @Override
    public String[] getCodes() {
        if (this.codes == null) {
            var paramsList = Arrays.asList(params);
            var codes = new String[this.params.length];

            for (int i = codes.length; i > 0; i--) {
                codes[codes.length - i] = CLASSNAME + "." + String.join(".", paramsList.subList(0, i));
            }
            this.codes = codes;
            return codes;
        }
        return this.codes;
    }

    public MessageSourceResolvable withArguments(Object... arguments) {
        return new DelegatingMessageSourceResolvableWithArguments(this, arguments);
    }

    @RequiredArgsConstructor
    private static class DelegatingMessageSourceResolvableWithArguments implements MessageSourceResolvable {

        private final MessageSourceResolvable resolvable;
        private final Object[] arguments;

        @Override
        public String[] getCodes() {
            return resolvable.getCodes();
        }

        @Override
        public Object[] getArguments() {
            return arguments;
        }
    }
}
