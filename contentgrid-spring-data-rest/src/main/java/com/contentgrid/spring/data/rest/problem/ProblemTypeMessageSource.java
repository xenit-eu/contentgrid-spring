package com.contentgrid.spring.data.rest.problem;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ResourceBundleMessageSource;

public final class ProblemTypeMessageSource extends ResourceBundleMessageSource {
    private ProblemTypeMessageSource() {
        setBasename("com.contentgrid.spring.data.rest.problem.messages");
    }

    public static MessageSourceAccessor getAccessor() {
        return new MessageSourceAccessor(new ProblemTypeMessageSource());
    }
}
