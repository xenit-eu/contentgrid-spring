package com.contentgrid.spring.data.rest.webmvc.mapping.typeinfo;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;
import javax.persistence.Embedded;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class TypeInformationProperty implements Property {
    private final Field field;
    private final TypeInformation<?> typeInformation;

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return Optional.ofNullable(field.getAnnotation(annotationClass));
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public TypeInformation<?> getTypeInformation() {
        return typeInformation;
    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    public Optional<Container> nestedContainer() {
        if(findAnnotation(Embedded.class).isPresent()) {
            return Optional.of(new TypeInformationContainer(typeInformation));
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
