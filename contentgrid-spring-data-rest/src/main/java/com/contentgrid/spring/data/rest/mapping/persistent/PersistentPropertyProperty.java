package com.contentgrid.spring.data.rest.mapping.persistent;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.data.rest.mapping.typeinfo.TypeInformationContainer;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class PersistentPropertyProperty implements Property {
    private final PersistentProperty<?> property;

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return Optional.ofNullable(property.findAnnotation(annotationClass));
    }

    @Override
    public String getName() {
        return property.getName();
    }

    @Override
    public TypeInformation<?> getTypeInformation() {
        return property.getTypeInformation();
    }

    @Override
    public boolean isRequired() {
        return Stream.of(
                        findAnnotation(Column.class).map(Column::nullable),
                        findAnnotation(OneToOne.class).map(OneToOne::optional),
                        findAnnotation(ManyToOne.class).map(ManyToOne::optional)
                )
                .flatMap(Optional::stream)
                .anyMatch(Predicate.isEqual(Boolean.FALSE));
    }

    @Override
    public boolean isUnique() {
        return findAnnotation(Column.class).map(Column::unique).orElse(false);
    }

    @Override
    public Optional<Container> nestedContainer() {
        if(findAnnotation(Embedded.class).isPresent()) {
            return Optional.of(new TypeInformationContainer(property.getTypeInformation()));
        }
        return Optional.empty();
    }

    @Override
    public boolean isIgnored() {
        return property.isVersionProperty();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
