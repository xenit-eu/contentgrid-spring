package eu.xenit.contentcloud.userapps.holmes.dcm.repository;

import eu.xenit.contentcloud.userapps.holmes.dcm.model.Case;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;


@RepositoryRestResource
interface CaseRepository extends JpaRepository<Case, UUID>, QuerydslPredicateExecutor<Case> {
}
