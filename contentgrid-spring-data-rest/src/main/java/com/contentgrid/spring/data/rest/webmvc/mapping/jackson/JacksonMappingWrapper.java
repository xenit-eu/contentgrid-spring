package com.contentgrid.spring.data.rest.webmvc.mapping.jackson;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;
import com.contentgrid.spring.data.rest.webmvc.mapping.wrapper.MappingWrapper;

public class JacksonMappingWrapper implements MappingWrapper {

    @Override
    public Container wrapContainer(Container container) {
        return container;
    }

    @Override
    public Property wrapProperty(Property property) {
        return new JacksonBasedProperty(property);
    }
}
