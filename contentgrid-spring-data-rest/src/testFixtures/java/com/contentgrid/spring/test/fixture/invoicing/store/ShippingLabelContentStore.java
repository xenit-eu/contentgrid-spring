package com.contentgrid.spring.test.fixture.invoicing.store;

import com.contentgrid.spring.test.fixture.invoicing.model.ShippingLabel;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.rest.StoreRestResource;

@StoreRestResource(path = "shipping-labels")
public interface ShippingLabelContentStore extends ContentStore<ShippingLabel, String> {

}
