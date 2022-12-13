package com.contentgrid.spring.test.fixture.invoicing.repository;

import com.contentgrid.spring.test.fixture.invoicing.model.Invoice;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>, QuerydslPredicateExecutor<Invoice> {

    Optional<Invoice> findByNumber(String number);

}
