package com.contentgrid.spring.data.rest.hal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.hateoas.UriTemplate;

class ContentGridCurieProviderTest {
    @Test
    void duplicateCurie_rejected() {
        var curieProvider = new ContentGridCurieProvider()
                .withCurie("test", UriTemplate.of("http://example.invalid/{rel}"));

        assertThatThrownBy(() -> {
            curieProvider.withCurie("test", UriTemplate.of("http://test.invalid/{rel}"));
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Curie prefix 'test' is already registered");

    }

}