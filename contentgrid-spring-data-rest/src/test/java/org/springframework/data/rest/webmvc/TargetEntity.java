package org.springframework.data.rest.webmvc;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.contentgrid.spring.querydsl.predicate.EntityId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Version;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.rest.core.annotation.RestResource;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class TargetEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    @Version
    private Long version = 0L;

    @JsonIgnore
    @RestResource(exported = false)
    @CollectionFilterParam(predicate = EntityId.class, documented = false)
    @ManyToMany(mappedBy = "items")
    private List<SourceEntity1> _internal_sourceEntity1__items;

    @JsonIgnore
    @RestResource(exported = false)
    @CollectionFilterParam(predicate = EntityId.class, documented = false)
    @ManyToMany(mappedBy = "items")
    private List<SourceEntity2> _internal_sourceEntity2__items;

}
