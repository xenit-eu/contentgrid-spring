package com.contentgrid.spring.data.rest.webmvc.mapping.persistent;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import com.contentgrid.spring.data.rest.webmvc.mapping.typeinfo.TypeInformationContainer;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
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
    public Optional<Container> nestedContainer() {
        if(findAnnotation(Embedded.class).isPresent()) {
            return Optional.of(new TypeInformationContainer(property.getTypeInformation()));
        }
        return Optional.empty();
    }

    @Override
    public boolean isIgnored() {
        return false;
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }
}
