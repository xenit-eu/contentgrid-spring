package com.contentgrid.spring.data.rest.webmvc.mapping.typeinfo;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class TypeInformationContainer implements Container {
    private final TypeInformation<?> typeInformation;

    @Override
    public TypeInformation<?> getTypeInformation() {
        return typeInformation;
    }

    @Override
    public void doWithProperties(Consumer<Property> handler) {
        for (Field field : typeInformation.getType().getDeclaredFields()) {
            handler.accept(new TypeInformationProperty(field, typeInformation.getRequiredProperty(field.getName())));
        }
    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {

    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return Optional.ofNullable(typeInformation.toTypeDescriptor().getAnnotation(annotationClass));
    }
}
