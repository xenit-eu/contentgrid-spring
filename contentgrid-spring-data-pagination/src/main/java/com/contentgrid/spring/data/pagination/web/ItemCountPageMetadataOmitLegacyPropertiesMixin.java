package com.contentgrid.spring.data.pagination.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Jackson mixin to optionally apply to {@link ItemCountPageMetadata} to omit legacy pagination properties
 */
@JsonIgnoreProperties({"totalElements", "totalPages"})
public interface ItemCountPageMetadataOmitLegacyPropertiesMixin {

}
