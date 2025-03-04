package org.springframework.data.rest.webmvc;

import com.contentgrid.spring.querydsl.annotation.CollectionFilterParam;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Version;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class SourceEntity1 {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @JsonProperty(access = Access.READ_ONLY)
    private UUID id;

    @Version
    private Long version = 0L;

    @ManyToMany
    @JoinTable(name = "source_entity_1__items", joinColumns = @JoinColumn(name = "source_entity1_id"), inverseJoinColumns = @JoinColumn(name = "target_entity_id"))
    @CollectionFilterParam("items")
    @JsonIgnore
    @org.springframework.data.rest.core.annotation.RestResource(rel = "d:items", path = "items")
    private List<TargetEntity> items;
}
