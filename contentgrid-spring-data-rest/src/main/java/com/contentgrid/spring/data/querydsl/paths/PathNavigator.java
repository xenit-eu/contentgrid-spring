package com.contentgrid.spring.data.querydsl.paths;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.dsl.BeanPath;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.PathInits;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldFilter;

/**
 * Navigates a {@link Path} similar to how {@link PathBuilder#get(String)} navigates a path,
 * except that it uses the generated (or proper) subtypes of {@link Path} for every accessed property
 */
@RequiredArgsConstructor
public class PathNavigator {
    @Getter
    private final Path<?> path;

    private Path<?> pathForExtension() {
        if(path instanceof CollectionPathBase<?, ?, ?> collectionPathBase) {
            return (Path<?>)collectionPathBase.any();
        }
        return path;
    }

    public Class<?> getTargetType() {
        return pathForExtension().getType();
    }

    public PathNavigator get(String propertyName) {
        var path = pathForExtension();
        var pathMetadata = PathMetadataFactory.forProperty(path, propertyName);

        return new PathNavigator(throughReflectionField(path, pathMetadata).orElseThrow(() -> new IllegalArgumentException("Path '%s' does not have property '%s'".formatted(path, propertyName))));
    }

    private Optional<Path<?>> throughReflectionField(Path<?> qInstance, PathMetadata expectedPathMetadata) {
        AtomicReference<Path<?>> matchedField = new AtomicReference<>();
        ReflectionUtils.doWithFields(qInstance.getClass(), field -> {
            var path = (Path<?>) ReflectionUtils.getField(field, qInstance);
            if(path == null) {
                // This case can happen when going more levels deep than requested by the pathinits set on the initial object.
                // Note that in for generated Q-classes, these are always references to another Q-class, so we can instantiate them here from path metadata
                path = initializePath(qInstance, field);
            }
            if(Objects.equals(expectedPathMetadata, path.getMetadata())) {
                matchedField.compareAndSet(null, path);
            }

        }, new ModifiersFieldFilter(Modifier::isPublic, m -> !Modifier.isStatic(m)).and(new TypeFieldFilter(Path.class)));

        return Optional.ofNullable(matchedField.get());

    }

    private static Path<?> initializePath(Path<?> qInstance, Field field) throws IllegalAccessException {
        var propertyMetadata = PathMetadataFactory.forProperty(qInstance, field.getName());
        try {
            if(EntityPathBase.class.isAssignableFrom(field.getType())) {
                var constructor = ReflectionUtils.accessibleConstructor(field.getType(), PathMetadata.class, PathInits.class);
                return (Path<?>) constructor.newInstance(propertyMetadata, PathInits.DIRECT);
            } else if(BeanPath.class.isAssignableFrom(field.getType())) {
                var constructor = ReflectionUtils.accessibleConstructor(field.getType(), PathMetadata.class);
                return (Path<?>) constructor.newInstance(propertyMetadata);
            } else {
                throw new IllegalStateException("Path '%s': field '%s' is null and can not be constructed".formatted(qInstance, field));
            }
        } catch (NoSuchMethodException|InvocationTargetException e) {
            ReflectionUtils.handleReflectionException(e);
            return null; // unreachable
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static class ModifiersFieldFilter implements FieldFilter {
        private final IntPredicate[] filters;

        public ModifiersFieldFilter(IntPredicate... filters) {
            this.filters = filters;
        }

        @Override
        public boolean matches(Field field) {
            var modifiers = field.getModifiers();
            for (IntPredicate filter : filters) {
                if(!filter.test(modifiers)) {
                    return false;
                }
            }

            return true;
        }
    }

    @RequiredArgsConstructor
    private static class TypeFieldFilter implements FieldFilter {
        private final Class<?> type;

        @Override
        public boolean matches(Field field) {
            return type.isAssignableFrom(field.getType());
        }
    }

}
