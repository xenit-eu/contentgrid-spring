package com.contentgrid.spring.test.fixture.invoicing.repository;

import com.contentgrid.spring.test.fixture.invoicing.model.ShippingLabel;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "shipping-labels", itemResourceRel = "d:shipping-label", collectionResourceRel = "d:shipping-labels")
public interface ShippingLabelRepository extends JpaRepository<ShippingLabel, UUID>, QuerydslPredicateExecutor<ShippingLabel> {

}
