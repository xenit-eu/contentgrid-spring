package com.contentgrid.spring.test.fixture.invoicing.store;

import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource
public interface CustomerContentStore extends ContentStore<Customer, String> {

}
