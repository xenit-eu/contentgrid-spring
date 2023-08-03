package com.contentgrid.spring.data.rest.links;

import org.springframework.hateoas.Links;

public interface ContentGridLinkCollector {

    Links getLinksFor(Object object, Links existing);

    Links getLinksForNested(Object object, Links existing);
}
