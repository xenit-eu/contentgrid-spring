package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.mapping.DomainTypeMapping;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedContainer;
import com.contentgrid.spring.data.rest.mapping.persistent.ThroughAssociationsContainer;
import com.contentgrid.spring.data.rest.mapping.rest.DataRestBasedContainer;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class EntityRepresentationModelAssembler implements
        RepresentationModelAssembler<RootResourceInformation, EntityRepresentationModel> {

    private final DomainTypeMapping domainTypeMapping;
    private final MessageResolver messageResolver;
    private final AttributeRepresentationModelAssembler attributeAssembler;
    private final RelationRepresentationModelAssembler relationAssembler;

    public EntityRepresentationModelAssembler(Repositories repositories, MessageResolver messageResolver,
            RepositoryRestConfiguration repositoryRestConfiguration, ResourceMappings resourceMappings,
            CollectionFiltersMapping collectionFiltersMapping) {
        this.domainTypeMapping = new DomainTypeMapping(repositories)
                .wrapWith(container -> new ThroughAssociationsContainer(container, repositories, 1))
                .wrapWith(DataRestBasedContainer::new)
//                .wrapWith(JacksonBasedContainer::new)
        ;
        this.messageResolver = messageResolver;
        this.attributeAssembler = new AttributeRepresentationModelAssembler(collectionFiltersMapping, messageResolver);
        this.relationAssembler = new RelationRepresentationModelAssembler(repositoryRestConfiguration, resourceMappings, messageResolver);
    }

    @Override
    public EntityRepresentationModel toModel(RootResourceInformation information) {
        var attributes = new ArrayList<AttributeRepresentationModel>();
        var relations = new ArrayList<RelationRepresentationModel>();

        var entityContainer = domainTypeMapping.forDomainType(information.getDomainType());

        entityContainer.doWithProperties(property -> {
            var attribute = attributeAssembler.toModel(information, List.of(property));
            attribute.ifPresent(attributes::add);
        });

        entityContainer.doWithAssociations(property -> {
            var relation = relationAssembler.toModel(information, property);
            relation.ifPresent(relations::add);
        });

        var name = entityContainer.findAnnotation(Table.class)
                .map(Table::name)
                .map(tableName -> tableName.replace("_", "-"))
                .orElse("");
        if (name.isEmpty()) {
            // TODO: still contains camelCase names
            name = StringUtils.uncapitalize(entityContainer.getTypeInformation().getType().getSimpleName());
        }

        return EntityRepresentationModel.builder()
                .name(name)
                .title(readTitle(information))
                .description(readDescription(information))
                .attributes(attributes)
                .relations(relations)
                .build();
    }

    private String readDescription(RootResourceInformation information) {
        var resolvable = new MessageSourceResolvable() {
            @Override
            public String[] getCodes() {
                return new String[]{information.getResourceMetadata().getItemResourceDescription().getMessage()};
            }

            @Override
            public String getDefaultMessage() {
                return ""; // Returns null if empty string (null [default] = throws exception)
            }
        };
        var description = messageResolver.resolve(resolvable);
        return description == null ? "" : description;
    }

    private String readTitle(RootResourceInformation information) {
        var resolvable = new MessageSourceResolvable() {
            @Override
            public String[] getCodes() {
                return new String[]{information.getDomainType().getName() + "._title"};
            }

            @Override
            public String getDefaultMessage() {
                return ""; // Returns null if empty string (null [default] = throws exception)
            }
        };
        return messageResolver.resolve(resolvable);
    }

}
