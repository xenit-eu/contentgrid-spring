package com.contentgrid.spring.audit.extractor;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.web.method.HandlerMethod;

@Builder
@Slf4j
public class HandlerMethodMatcher {

    @NonNull
    private final Class<?> type;

    @Singular
    @NonNull
    private final Set<String> methodNames;

    private HandlerMethodMatcher(Class<?> type, Set<String> methodNames) {
        this.type = type;
        this.methodNames = Set.copyOf(methodNames);
        if (type == Void.TYPE) {
            return;
        }
        var methodNameValidation = new HashSet<>(methodNames);

        for (Method method : type.getDeclaredMethods()) {
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

        public HandlerMethodMatcherBuilder className(String className) {
            Class<?> type = Void.TYPE;
            try {
                type = Class.forName(className);
            } catch (ClassNotFoundException classNotFoundException) {
                log.warn("Class {} was not found", className);
            }
            return type(type);
        }

        @SneakyThrows
        public HandlerMethodMatcherBuilder allMethods() {
            for (Method method : type.getDeclaredMethods()) {
                methodName(method.getName());
            }
            return this;
        }
    }

}
