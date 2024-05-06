package com.contentgrid.spring.test.fixture.invoicing.repository;

import com.contentgrid.spring.test.fixture.invoicing.model.Label;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(path = "labels", itemResourceRel = "d:label", collectionResourceRel = "d:labels")
public interface LabelRepository extends JpaRepository<Label, UUID>, QuerydslPredicateExecutor<Label> {

}
