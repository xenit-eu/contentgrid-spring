package org.springframework.data.rest.webmvc;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(itemResourceRel = "d:source_entity2", collectionResourceRel = "d:source_entity2s", path = "source-entity2s")
public interface SourceEntity2Repository extends JpaRepository<SourceEntity2, UUID>, QuerydslPredicateExecutor<SourceEntity2> {

}
