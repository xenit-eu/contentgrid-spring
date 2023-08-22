package com.contentgrid.spring.data.querydsl.paths;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.types.PathMetadataFactory;
import com.querydsl.core.types.dsl.CollectionPathBase;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.SimpleExpression;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.ClassUtils;
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

    public PathNavigator get(String propertyName) {
        var path = pathForExtension();
        var pathMetadata = PathMetadataFactory.forProperty(path, propertyName);

        return new PathNavigator(throughReflectionField(path, pathMetadata).orElseThrow(() -> new IllegalArgumentException("Path '%s' does not have property '%s'".formatted(path, propertyName))));
    }

    private Optional<Path<?>> throughReflectionField(Object qInstance, PathMetadata expectedPathMetadata) {
        AtomicReference<Path<?>> matchedField = new AtomicReference<>();
        ReflectionUtils.doWithFields(qInstance.getClass(), field -> {
            var path = (Path<?>) ReflectionUtils.getField(field, qInstance);
            if(Objects.equals(expectedPathMetadata, path.getMetadata())) {
                matchedField.compareAndSet(null, path);
            }

        }, new ModifiersFieldFilter(Modifier::isPublic, m -> !Modifier.isStatic(m)).and(new TypeFieldFilter(Path.class)));

        return Optional.ofNullable(matchedField.get());

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
