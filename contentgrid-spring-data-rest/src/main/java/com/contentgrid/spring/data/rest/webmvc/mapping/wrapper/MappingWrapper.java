package com.contentgrid.spring.data.rest.webmvc.mapping.wrapper;

import com.contentgrid.spring.data.rest.webmvc.mapping.Container;
import com.contentgrid.spring.data.rest.webmvc.mapping.Property;

public interface MappingWrapper {
    Container wrapContainer(Container container);
    Property wrapProperty(Property property);
}
