package com.contentgrid.spring.test.fixture.invoicing.repository;

import com.contentgrid.spring.test.fixture.invoicing.model.ShippingAddress;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "shipping-addresses", itemResourceRel = "d:shipping-address", collectionResourceRel = "d:shipping-addresses")
public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, UUID>, QuerydslPredicateExecutor<ShippingAddress> {

}
