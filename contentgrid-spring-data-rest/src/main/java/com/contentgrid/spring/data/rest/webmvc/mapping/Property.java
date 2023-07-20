package com.contentgrid.spring.data.rest.webmvc.mapping;

import java.util.Optional;
import org.springframework.data.util.TypeInformation;

public interface Property extends Annotated {
    String getName();
    TypeInformation<?> getTypeInformation();

    boolean isIgnored();
    boolean isRequired();
    boolean isReadOnly();

    Optional<Container> nestedContainer();
}
