package com.contentgrid.spring.data.rest.webmvc.blueprint;

import com.contentgrid.spring.data.rest.mapping.Container;
import com.contentgrid.spring.data.rest.mapping.Property;
import com.contentgrid.spring.data.rest.mapping.jackson.JacksonBasedProperty;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.ProfileController;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.MessageResolver;

@RequiredArgsConstructor
public class RelationRepresentationModelAssembler {

    private final RepositoryRestConfiguration repositoryRestConfiguration;
    private final ResourceMappings resourceMappings;
    private final MessageResolver messageResolver;

    public Optional<RelationRepresentationModel> toModel(RootResourceInformation information, Property property) {
        var jsonProperty = new JacksonBasedProperty(property);
        if (property.isIgnored()) {
            // using property instead of jsonProperty because jsonProperty ignores one-to-many and many-to-many relations
            return Optional.empty();
        }

        var isManySourcePerTarget = Stream.of(ManyToOne.class, ManyToMany.class)
                .anyMatch(annotation -> jsonProperty.findAnnotation(annotation).isPresent());
        var isManyTargetPerSource = Stream.of(OneToMany.class, ManyToMany.class)
                .anyMatch(annotation -> jsonProperty.findAnnotation(annotation).isPresent());

        var relationModel = RelationRepresentationModel.builder()
                .name(jsonProperty.getName())
                .title(readTitle(information, property))
                .description(readDescription(information, property))
                .manySourcePerTarget(isManySourcePerTarget)
                .manyTargetPerSource(isManyTargetPerSource)
                .required(jsonProperty.isRequired())
                .build();
        var targetType = property.nestedContainer()
                .<TypeInformation<?>>map(Container::getTypeInformation)
                .orElseThrow();

        var profileUrl = ProfileController.getPath(repositoryRestConfiguration,
                resourceMappings.getMetadataFor(targetType.getType()));

        relationModel.add(Link.of(profileUrl, BlueprintLinkRelations.TARGET_ENTITY));

        return Optional.of(relationModel);
    }

    private String readDescription(RootResourceInformation information, Property property) {
        var resolvable = new MessageSourceResolvable() {
            @Override
            public String[] getCodes() {
                // TODO: nested properties
                return new String[]{information.getResourceMetadata().getItemResourceDescription().getMessage() + "." + property.getName()};
            }

            @Override
            public String getDefaultMessage() {
                return ""; // Returns null if empty string (null [default] = throws exception)
            }
        };
        var description = messageResolver.resolve(resolvable);
        return description == null ? "" : description;
    }

    private String readTitle(RootResourceInformation information, Property property) {
        var resolvable = new MessageSourceResolvable() {
            @Override
            public String[] getCodes() {
                // TODO: nested properties
                return new String[]{information.getDomainType().getName() + "." + property.getName() + "._title"};
            }

            @Override
            public String getDefaultMessage() {
                return ""; // Returns null if empty string (null [default] = throws exception)
            }
        };
        return messageResolver.resolve(resolvable);
    }

}
