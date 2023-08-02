package com.contentgrid.spring.data.rest.hal;

import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.CurieProvider;

/**
 * Fluent builder for {@link CurieProvider}
 */
public interface CurieProviderBuilder {

    /**
     * Adds a mapping from CURIE prefix to a {@link UriTemplate} for resolving the CURIE against
     * @param prefix CURIE prefix
     * @param template Template to use to resolve the CURIE
     * @return Copy with new curie mapping applied
     */
    CurieProviderBuilder withCurie(String prefix, UriTemplate template);

    /**
     * Builds a {@link CurieProvider} with the mappings specified in the builder
     * @return An immutable {@link CurieProvider} instance
     */
    CurieProvider build();
}
