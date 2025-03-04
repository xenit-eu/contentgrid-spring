package org.springframework.data.rest.webmvc;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(itemResourceRel = "d:source_entity1", collectionResourceRel = "d:source_entity1s", path = "source-entity1s")
public interface SourceEntity1Repository extends JpaRepository<SourceEntity1, UUID>, QuerydslPredicateExecutor<SourceEntity1> {

}
