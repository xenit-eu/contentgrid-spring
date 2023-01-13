package com.contentgrid.spring.data.querydsl;

import com.querydsl.core.types.Path;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class QuerydslBindingsInspector {

    @NonNull
    private final QuerydslBindings querydslBindings;

    private static final Method METHOD_GETPROPERTYPATH;

    private static final Method METHOD_ISPATHAVAILABLE;

    static {
        try {
            METHOD_GETPROPERTYPATH = QuerydslBindings.class.getDeclaredMethod("getPropertyPath", String.class,
                    TypeInformation.class);
            METHOD_GETPROPERTYPATH.setAccessible(true);

            METHOD_ISPATHAVAILABLE = QuerydslBindings.class.getDeclaredMethod("isPathAvailable", String.class, Class.class);
            METHOD_ISPATHAVAILABLE.trySetAccessible();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<String> findPathBindingFor(Path<?> path) {
        return this.findPathBindingFor(path.getMetadata().getName(), path.getRoot().getType());
    }
    public Optional<String> findPathBindingFor(String path, Class<?> type) {
        if (this.isQuerydslPathBindingAvailable(path, type)) {
            return Optional.of(path);
        }

        // property-path with identical name has been hidden
        // can we enumerate the aliases and check they bind to the type we are expecting ??


        return Optional.empty();
    }

    private Object getPropertyPath(String dotPath, ClassTypeInformation<?> typeInfo) {
        try {
            return METHOD_GETPROPERTYPATH.invoke(this.querydslBindings, dotPath, typeInfo);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isQuerydslPathBindingAvailable(String path, Class<?> type) {
        try {
            return Boolean.TRUE.equals(METHOD_ISPATHAVAILABLE.invoke(this.querydslBindings, path, type));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}
