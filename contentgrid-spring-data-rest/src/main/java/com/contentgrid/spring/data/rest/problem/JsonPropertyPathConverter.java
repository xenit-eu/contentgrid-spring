package com.contentgrid.spring.data.rest.problem;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedProperty;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JsonPropertyPathConverter {

    private final DomainTypeMapping domainTypeMapping;
    private final Map<CacheKey, String> fromJavaPropertyPathCache = new ConcurrentHashMap<>();

    public String fromJavaPropertyPath(Class<?> domainType, String javaPropertyPath) {
        return fromJavaPropertyPathCache.computeIfAbsent(
                new CacheKey(domainType, javaPropertyPath),
                this::createFromJavaPropertyPath
        );
    }

    private String createFromJavaPropertyPath(CacheKey key) {
        return convertPropertyPath(
                domainTypeMapping.forDomainType(key.domainType()),
                key.propertyPath().split("\\.")
        )
                .map(JacksonBasedProperty::new)
                // If any property in the chain is ignored, it doesn't have a name
                .map(prop -> prop.isIgnored() ? null : prop.getName())
                .reduce("", (a, b) -> {
                    // If there is any ignored property in the chain, we refuse to create a property path
                    if (a == null || b == null) {
                        return null;
                    }
                    if (a.isEmpty()) {
                        return b;
                    } else if (b.isEmpty()) {
                        return a;
                    } else {
                        return a + "." + b;
                    }
                });
    }

    private Stream<Property> convertPropertyPath(Container topContainer, String[] propertyPath) {
        Optional<Container> maybeContainer = Optional.of(topContainer);
        Stream.Builder<Property> properties = Stream.builder();

        for (String propertyName : propertyPath) {
            var container = maybeContainer.orElseThrow(
                    () -> createPropertyPathException(propertyPath, properties, "no container"));
            var property = findProperty(container, propertyName);
            if (property == null) {
                throw createPropertyPathException(propertyPath, properties, "property not found");
            }
            properties.add(property);
            maybeContainer = property.nestedContainer();
        }

        return properties.build();
    }

    private Property findProperty(Container container, String propertyName) {
        AtomicReference<Property> found = new AtomicReference<>();
        container.doWithAll(property -> {
            if (Objects.equals(property.getName(), propertyName)) {
                found.set(property);
            }
        });

        return found.get();
    }

    private IllegalStateException createPropertyPathException(String[] propertyPath, Stream.Builder<Property> builder,
            String msg) {
        return new IllegalStateException("Can not expand property path '%s' at position %d: %s".formatted(
                String.join(".", propertyPath),
                builder.build().count(),
                msg
        ));
    }

    private record CacheKey(Class<?> domainType, String propertyPath) {

    }

}
