package com.contentgrid.automations;


import lombok.experimental.UtilityClass;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.UriTemplate;
import org.springframework.hateoas.mediatype.hal.HalLinkRelation;

@UtilityClass
public class AutomationLinkRelations {
    static final String CURIE = "automation";
    static final UriTemplate TEMPLATE = UriTemplate.of("https://contentgrid.cloud/rels/automation/{rel}");

    public static final LinkRelation ANNOTATION = HalLinkRelation.curied(CURIE, "annotation");
    public static final String ANNOTATION_STRING = CURIE+":annotation";
    public static final LinkRelation TARGET_ENTITY = HalLinkRelation.curied(CURIE, "target-entity");

}
