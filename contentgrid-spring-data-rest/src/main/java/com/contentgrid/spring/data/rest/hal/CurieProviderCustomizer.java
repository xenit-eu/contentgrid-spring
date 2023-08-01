package com.contentgrid.spring.data.rest.hal;

import org.springframework.hateoas.UriTemplate;

/**
 * Customizer interface for {@link CurieProviderBuilder}.
 * <p>
 * By implementing this interface and exposing them as beans, they will automatically be applied to the default {@link org.springframework.hateoas.mediatype.hal.CurieProvider}
 */
@FunctionalInterface
public interface CurieProviderCustomizer {

    /**
     * Customize the {@link CurieProviderBuilder}
     * @param builder current builder
     * @return builder with the desired customizations applied
     */
    CurieProviderBuilder customize(CurieProviderBuilder builder);

    /**
     * Register a mapping between CURIE prefix and a URI template
     *
     * @param curiePrefix The CURIE prefix
     * @param template The URI template to use
     * @return customizer that registers a CURIE prefix mapping
     *
     * @see CurieProviderBuilder#withCurie(String, UriTemplate) for the builder method
     * @see #register(String, String) for a shortcut that does not require a {@link UriTemplate} instance
     */
    static CurieProviderCustomizer register(String curiePrefix, UriTemplate template) {
        return builder -> builder.withCurie(curiePrefix, template);
    }

    /**
     * Register a mapping between CURIE prefix and a URI template
     *
     * @param curiePrefix The CURIE prefix
     * @param template The URI template to use
     * @return customizer that registers a CURIE prefix mapping
     *
     * @see CurieProviderBuilder#withCurie(String, UriTemplate) for the builder method
     * @see #register(String, UriTemplate) for passing a {@link UriTemplate} directly
     */
    static CurieProviderCustomizer register(String curiePrefix, String template) {
        return register(curiePrefix, UriTemplate.of(template));
    }
}
