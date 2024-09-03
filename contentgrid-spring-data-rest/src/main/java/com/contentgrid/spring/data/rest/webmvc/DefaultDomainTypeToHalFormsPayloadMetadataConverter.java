package com.contentgrid.spring.data.rest.webmvc;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.With;
import org.springframework.hateoas.AffordanceModel.InputPayloadMetadata;
import org.springframework.hateoas.AffordanceModel.Named;
import org.springframework.hateoas.AffordanceModel.PayloadMetadata;
import org.springframework.hateoas.AffordanceModel.PropertyMetadata;
import org.springframework.hateoas.mediatype.html.HtmlInputType;
import org.springframework.http.MediaType;

@RequiredArgsConstructor
public class DefaultDomainTypeToHalFormsPayloadMetadataConverter implements
        DomainTypeToHalFormsPayloadMetadataConverter {

    private final Collection<HalFormsPayloadMetadataContributor> contributors;

    private Stream<PropertyMetadata> callContributors(Class<?> domainType, BiFunction<HalFormsPayloadMetadataContributor, Class<?>, Stream<PropertyMetadata>> contribFunction) {
        return contributors.stream()
                .flatMap(contributor -> contribFunction.apply(contributor, domainType));
    }

    @Override
    public PayloadMetadata convertToCreatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();

        callContributors(domainType, HalFormsPayloadMetadataContributor::contributeToCreateForm)
                .forEachOrdered(properties::add);

        var hasFiles = properties.stream().anyMatch(prop -> Objects.equals(HtmlInputType.FILE_VALUE, prop.getInputType()));

        return new ClassnameI18nedPayloadMetadata(domainType, properties)
                .withMediaTypes(List.of(hasFiles?MediaType.MULTIPART_FORM_DATA:MediaType.APPLICATION_JSON));
    }

    @Override
    public PayloadMetadata convertToUpdatePayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();

        callContributors(domainType, HalFormsPayloadMetadataContributor::contributeToUpdateForm)
                .forEachOrdered(properties::add);

        return new ClassnameI18nedPayloadMetadata(domainType, properties)
                .withMediaTypes(List.of(MediaType.APPLICATION_JSON));
    }

    @Override
    public PayloadMetadata convertToSearchPayloadMetadata(Class<?> domainType) {
        List<PropertyMetadata> properties = new ArrayList<>();

        callContributors(domainType, HalFormsPayloadMetadataContributor::contributeToSearchForm)
                .forEachOrdered(properties::add);

        return new ClassnameI18nedPayloadMetadata(domainType, properties);
    }


    @AllArgsConstructor
    @RequiredArgsConstructor
    private static class ClassnameI18nedPayloadMetadata implements InputPayloadMetadata {
        private final Class<?> domainType;
        private final Collection<PropertyMetadata> properties;
        @With
        private List<MediaType> mediaTypes = Collections.emptyList();

        @Override
        public <T extends Named> T customize(T target, Function<PropertyMetadata, T> customizer) {
            return properties.stream()
                    .filter(propMeta -> propMeta.getName().equals(target.getName()))
                    .findAny()
                    .map(customizer)
                    .orElse(target);
        }

        @Override
        public List<String> getI18nCodes() {
            return List.of(domainType.getName());
        }

        @Override
        public List<MediaType> getMediaTypes() {
            return this.mediaTypes;
        }

        @Override
        public Stream<PropertyMetadata> stream() {
            return properties.stream();
        }

        @Override
        public Class<?> getType() {
            return this.domainType;
        }
    }

}
