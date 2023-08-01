package com.contentgrid.spring.data.rest.hal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;

class ContentGridCurieProviderTest {

    public static final CurieProviderBuilder CURIE_PROVIDER_BUILDER = new ContentGridCurieProvider()
            .withCurie("test", UriTemplate.of("http://example.invalid/{rel}"));

    @Test
    void withCurie_duplicate() {
        assertThatThrownBy(() -> {
            CURIE_PROVIDER_BUILDER.withCurie("test", UriTemplate.of("http://test.invalid/{rel}"));
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CURIE prefix 'test' is already registered");
    }

    @Test
    void withCurie_ianaScheme() {
        assertThatThrownBy(() -> {
            CURIE_PROVIDER_BUILDER.withCurie("http", UriTemplate.of("http://test.invalid/{rel}"));
        }).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CURIE prefix 'http' can not be an IANA-registered URI scheme");
    }

    @ParameterizedTest
    @CsvSource({
            "self",
            "test:abc",
            "http://example.com/rels/xyz"
    })
    void getNamespacedRelFor_valid(String rel) {
        assertThat(CURIE_PROVIDER_BUILDER.build().getNamespacedRelFor(LinkRelation.of(rel)))
                .extracting(HalLinkRelation::value)
                .isEqualTo(rel);
    }

    @ParameterizedTest
    @CsvSource({
            "some-unregistered-thing",
            "nocurie:abc",
            "nocurie://xyz/def"
    })
    void getNamespacedRelFor_invalid(String rel) {
        var curieProvider = CURIE_PROVIDER_BUILDER.build();

        assertThatThrownBy(() -> curieProvider.getNamespacedRelFor(LinkRelation.of(rel)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Relation '%s'".formatted(rel));
    }

}