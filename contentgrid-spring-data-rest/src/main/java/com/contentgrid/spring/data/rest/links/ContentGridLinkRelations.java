package com.contentgrid.spring.data.rest.links;

import lombok.experimental.UtilityClass;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;

@UtilityClass
public class ContentGridLinkRelations {
    static final String CURIE = "cg";
    static final UriTemplate TEMPLATE = UriTemplate.of("https://contentgrid.cloud/rels/contentgrid/{rel}");

    public static final LinkRelation ENTITY = HalLinkRelation.curied(CURIE, "entity");
    public static final LinkRelation RELATION = HalLinkRelation.curied(CURIE, "relation");
    public static final LinkRelation CONTENT = HalLinkRelation.curied(CURIE, "content");
    public static final LinkRelation AUTOMATION_ANNOTATION = HalLinkRelation.curied(CURIE, "automation-annotation");
    public static final LinkRelation ENTITY_PROFILE = HalLinkRelation.curied(CURIE, "entity-profile");

}
