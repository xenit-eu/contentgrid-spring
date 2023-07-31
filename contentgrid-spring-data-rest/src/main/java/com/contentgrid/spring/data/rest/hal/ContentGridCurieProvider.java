package com.contentgrid.spring.data.rest.hal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.Links;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;

@RequiredArgsConstructor
class ContentGridCurieProvider implements CurieProvider, CurieProviderBuilder {

    private final Map<String, UriTemplate> curies;

    public ContentGridCurieProvider() {
        this(Map.of());
    }

    @Override
    public HalLinkRelation getNamespacedRelFrom(Link link) {
        return getNamespacedRelFor(link.getRel());
    }

    @Override
    public HalLinkRelation getNamespacedRelFor(LinkRelation rel) {
        return HalLinkRelation.of(rel);
    }

    @Override
    public Collection<?> getCurieInformation(Links links) {
        return curies.entrySet().stream()
                .map(it -> createCurieLink(it.getKey(), it.getValue()))
                .toList();
    }

    private Link createCurieLink(String name, UriTemplate template) {
        return Link.of(
                template,
                HalLinkRelation.CURIES
        ).withName(name);
    }

    @Override
    public CurieProviderBuilder withCurie(String prefix, UriTemplate template) {
        if(curies.containsKey(prefix)) {
            throw new IllegalArgumentException("Curie prefix '%s' is already registered with template '%s' and can not be re-registered with template '%s'.".formatted(
                    prefix,
                    curies.get(prefix),
                    template
            ));
        }
        var curies = new HashMap<>(this.curies);
        curies.put(prefix, template);
        return new ContentGridCurieProvider(curies);
    }

    @Override
    public CurieProvider build() {
        return this;
    }
}
