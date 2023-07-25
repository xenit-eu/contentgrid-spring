package com.contentgrid.spring.data.rest.webmvc.mapping.persistent;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.SimpleAssociationHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.util.TypeInformation;

@RequiredArgsConstructor
public class PersistentEntityContainer implements Container {
    private final PersistentEntity<?, ?> persistentEntity;

    @Override
    public TypeInformation<?> getTypeInformation() {
        return persistentEntity.getTypeInformation();
    }

    @Override
    public void doWithProperties(Consumer<Property> handler) {
        persistentEntity.doWithProperties((SimplePropertyHandler) property -> {
            handler.accept(new PersistentPropertyProperty(property));
        });

    }

    @Override
    public void doWithAssociations(Consumer<Property> handler) {
        persistentEntity.doWithAssociations((SimpleAssociationHandler) association -> {
            handler.accept(new PersistentPropertyProperty(association.getInverse()));
        });
    }

    @Override
    public <A extends Annotation> Optional<A> findAnnotation(Class<A> annotationClass) {
        return Optional.ofNullable(persistentEntity.findAnnotation(annotationClass));
    }
}
