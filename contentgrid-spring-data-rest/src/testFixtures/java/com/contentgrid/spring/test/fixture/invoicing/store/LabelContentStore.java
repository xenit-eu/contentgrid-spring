package com.contentgrid.spring.test.fixture.invoicing.store;

import com.contentgrid.spring.test.fixture.invoicing.model.Label;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource
public interface LabelContentStore extends ContentStore<Label, String> {

}
