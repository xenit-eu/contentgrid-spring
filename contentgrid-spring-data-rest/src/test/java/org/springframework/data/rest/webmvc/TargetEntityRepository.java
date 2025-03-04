package org.springframework.data.rest.webmvc;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(itemResourceRel = "d:target_entity", collectionResourceRel = "d:target_entity", path = "target-entities")
public interface TargetEntityRepository extends JpaRepository<TargetEntity, UUID>, QuerydslPredicateExecutor<TargetEntity> {

}
