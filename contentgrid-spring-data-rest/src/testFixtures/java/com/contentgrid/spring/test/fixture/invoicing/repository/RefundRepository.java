package com.contentgrid.spring.test.fixture.invoicing.repository;

import com.contentgrid.spring.test.fixture.invoicing.model.Refund;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(itemResourceRel = "d:refund", collectionResourceRel = "d:refunds")
public interface RefundRepository extends JpaRepository<Refund, UUID>, QuerydslPredicateExecutor<Refund> {

}
