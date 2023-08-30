package com.contentgrid.spring.data.rest.links;

import lombok.RequiredArgsConstructor;
import org.springframework.data.rest.webmvc.mapping.LinkCollector;
import org.springframework.hateoas.Links;

@RequiredArgsConstructor
class AggregateLinkCollector implements LinkCollector {
    private final LinkCollector delegate;
    private final Iterable<ContentGridLinkCollector> collectors;

    @Override
    public Links getLinksFor(Object object) {
        return getLinksFor(object, Links.NONE);
    }

    @Override
    public Links getLinksFor(Object object, Links existing) {
        existing = delegate.getLinksFor(object, existing);
        for (var collector : collectors) {
            existing = collector.getLinksFor(object, existing);
        }
        return existing;
    }

    @Override
    public Links getLinksForNested(Object object, Links existing) {
        existing = delegate.getLinksForNested(object, existing);
        for (var collector : collectors) {
            existing = collector.getLinksForNested(object, existing);
        }
        return existing;
    }
}
