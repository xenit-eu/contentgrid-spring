package com.contentgrid.spring.test.fixture.invoicing.repository;

import com.contentgrid.spring.test.fixture.invoicing.model.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface CustomerRepository extends JpaRepository<Customer, UUID>, QuerydslPredicateExecutor<Customer> {

    Optional<Customer> findFirstByName(String name);

    Optional<Customer> findByVat(String vat);
}
