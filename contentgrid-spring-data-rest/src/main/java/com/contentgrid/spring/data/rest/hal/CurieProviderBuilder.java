package com.contentgrid.spring.data.rest.hal;

import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.CurieProvider;

public interface CurieProviderBuilder {
    CurieProviderBuilder withCurie(String prefix, UriTemplate template);
    CurieProvider build();
}
