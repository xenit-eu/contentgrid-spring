package com.contentgrid.spring.data.rest.webmvc.mapping;

import java.util.function.Consumer;
import org.springframework.data.util.TypeInformation;

public interface Container extends Annotated {
    TypeInformation<?> getTypeInformation();
    default void doWithAll(Consumer<Property> handler) {
        doWithProperties(handler);
        doWithAssociations(handler);
    }
    void doWithProperties(Consumer<Property> handler);
    void doWithAssociations(Consumer<Property> handler);
}
