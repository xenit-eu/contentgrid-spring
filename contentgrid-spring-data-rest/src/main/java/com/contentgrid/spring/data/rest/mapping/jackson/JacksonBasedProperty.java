package com.contentgrid.spring.data.rest.mapping.jackson;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class JacksonBasedProperty implements Property {
    private final Property delegate;

    protected Optional<String> preferredName() {
        return delegate.findAnnotation(JsonProperty.class)
                .map(JsonProperty::value)
                .filter(Predicate.not(Predicate.isEqual(JsonProperty.USE_DEFAULT_NAME)));
    }

    @Override
    public String getName() {
        return preferredName().orElseGet(delegate::getName);
    }

    @Override
    public TypeInformation<?> getTypeInformation() {
        return delegate.getTypeInformation();
    }

    @Override
    public boolean isIgnored() {
        return delegate.isIgnored() || delegate.findAnnotation(JsonIgnore.class).isPresent();
    }

    @Override
    public boolean isRequired() {
        return delegate.isRequired();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly() || delegate.findAnnotation(JsonProperty.class)
                .map(prop -> prop.access() == Access.READ_ONLY)
                .orElse(false);
    }

    @Override
    public Optional<Container> nestedContainer() {
        return delegate.nestedContainer()
                .map(JacksonBasedContainer::new);
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return delegate.findAnnotation(annotationClass);
    }
}
