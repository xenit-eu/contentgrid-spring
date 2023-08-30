package com.contentgrid.spring.data.rest.hal;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.IanaUriSchemes;
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
        assertRegisteredCurie(rel);
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
            throw new IllegalArgumentException("CURIE prefix '%s' is already registered with template '%s' and can not be re-registered with template '%s'.".formatted(
                    prefix,
                    curies.get(prefix),
                    template
            ));
        }
        if(IanaUriSchemes.isIanaUriScheme(prefix)) {
            throw new IllegalArgumentException("CURIE prefix '%s' can not be an IANA-registered URI scheme.".formatted(prefix));
        }
        var curies = new HashMap<>(this.curies);
        curies.put(prefix, template);
        return new ContentGridCurieProvider(curies);
    }

    @Override
    public CurieProvider build() {
        return this;
    }

    private void assertRegisteredCurie(LinkRelation rel) {
        var relation = rel.value();
        int firstColonIndex = relation.indexOf(':');

        String curie = firstColonIndex == -1 ? null : relation.substring(0, firstColonIndex);

        if(curie == null) {
            // Not curie -> need to check if it's a registered link relation
            if(!IanaLinkRelations.isIanaRel(relation) && !HalLinkRelation.CURIES.isSameAs(rel)) {
                throw new IllegalArgumentException("Relation '%s' is not an IANA-registered relation".formatted(relation));
            }
            return;
        }

        if(IanaUriSchemes.isIanaUriScheme(curie)) {
            // Not a curie, but a RFC 5988 #4.2a extension relation type
            return;
        }

        if(!curies.containsKey(curie)) {
            throw new IllegalArgumentException("Relation '%s' uses CURIE that is not registered".formatted(relation));
        }
    }
}
