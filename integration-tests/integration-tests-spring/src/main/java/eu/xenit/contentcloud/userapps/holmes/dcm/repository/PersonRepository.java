package eu.xenit.contentcloud.userapps.holmes.dcm.repository;

import eu.xenit.contentcloud.userapps.holmes.dcm.model.Person;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
interface PersonRepository extends JpaRepository<Person, UUID> {
}
