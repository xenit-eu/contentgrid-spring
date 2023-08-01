package com.contentgrid.spring.data.rest.hal;

import org.springframework.hateoas.UriTemplate;

public interface CurieProviderCustomizer {
    CurieProviderBuilder customize(CurieProviderBuilder builder);

    static CurieProviderCustomizer register(String curiePrefix, UriTemplate template) {
        return builder -> builder.withCurie(curiePrefix, template);
    }

    static CurieProviderCustomizer register(String curiePrefix, String template) {
        return register(curiePrefix, UriTemplate.of(template));
    }
}
