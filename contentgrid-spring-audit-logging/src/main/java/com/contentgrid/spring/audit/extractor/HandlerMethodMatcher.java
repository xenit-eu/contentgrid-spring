package com.contentgrid.spring.audit.extractor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.SneakyThrows;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

@Builder
public class HandlerMethodMatcher {

    @Nullable
    private final Class<?> type;

    @Singular
    @NonNull
    private final Set<String> methodNames;

    private HandlerMethodMatcher(Class<?> type, Set<String> methodNames) {
        this.type = type;
        this.methodNames = Set.copyOf(methodNames);
        var methodNameValidation = new HashSet<>(methodNames);

        for (Method method : type.getMethods()) {
            methodNameValidation.remove(method.getName());
        }

        if (!methodNameValidation.isEmpty()) {
            throw new IllegalArgumentException("Methods %s are missing on %s".formatted(
                    String.join(", ", methodNameValidation),
                    type
            ));
        }
    }

    public boolean matches(Object maybeHandlerMethod) {
        if (maybeHandlerMethod instanceof HandlerMethod handlerMethod) {
            return matches(handlerMethod);
        }
        return false;
    }

    public boolean matches(HandlerMethod handlerMethod) {
        if (!type.isAssignableFrom(handlerMethod.getBeanType())) {
            return false;
        }

        return methodNames.contains(handlerMethod.getMethod().getName());
    }

    public static class HandlerMethodMatcherBuilder {

        @SneakyThrows
        public HandlerMethodMatcherBuilder className(String className) {
            return type(Class.forName(className));
        }
    }

}
