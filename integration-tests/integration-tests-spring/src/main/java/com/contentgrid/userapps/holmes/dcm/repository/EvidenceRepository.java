package com.contentgrid.userapps.holmes.dcm.repository;

import com.contentgrid.userapps.holmes.dcm.model.Evidence;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
interface EvidenceRepository extends JpaRepository<Evidence, UUID>, QuerydslPredicateExecutor<Evidence> {
}
