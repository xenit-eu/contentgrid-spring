package org.springframework.data.rest.webmvc;

import static org.springframework.data.querydsl.QuerydslUtils.QUERY_DSL_PRESENT;
import static org.springframework.data.rest.webmvc.RestMediaTypes.SPRING_DATA_COMPACT_JSON_VALUE;
import static org.springframework.data.rest.webmvc.RestMediaTypes.TEXT_URI_LIST_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.PATCH;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import com.contentgrid.spring.data.querydsl.QuerydslBindingsInspector;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mapping.IdentifierAccessor;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBindingsFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvoker;
import org.springframework.data.rest.core.mapping.PropertyAwareResourceMapping;
import org.springframework.data.rest.core.mapping.RepositoryResourceMappings;
import org.springframework.data.rest.core.mapping.ResourceMetadata;
import org.springframework.data.rest.core.support.SelfLinkProvider;
import org.springframework.data.rest.webmvc.support.BackendId;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RepositoryRestController
public class DelegatingRepositoryPropertyReferenceController {

    private static final String BASE_MAPPING = "/{repository}/{id}/{property}";

    private final RepositoryPropertyReferenceController delegate;
    private final Repositories repositories;
    private final RepositoryResourceMappings mappings;

    private final RepositoryEntityLinks entityLinks;

    private final SelfLinkProvider selfLinkProvider;

    private final ObjectProvider<QuerydslBindingsFactory> querydslBindingsFactoryProvider;

    private final ContentGridRestProperties contentGridRestProperties;

    @Autowired
    DelegatingRepositoryPropertyReferenceController(RepositoryPropertyReferenceController delegate,
            Repositories repositories, RepositoryResourceMappings mappings,
            RepositoryEntityLinks entityLinks, SelfLinkProvider selfLinkProvider,
            ObjectProvider<QuerydslBindingsFactory> querydslBindingsFactoryProvider,
            ContentGridRestProperties contentGridRestProperties) {

        this.delegate = delegate;

        this.repositories = repositories;
        this.mappings = mappings;
        this.entityLinks = entityLinks;
        this.selfLinkProvider = selfLinkProvider;
        this.querydslBindingsFactoryProvider = querydslBindingsFactoryProvider;
        this.contentGridRestProperties = contentGridRestProperties;
    }

    @RequestMapping(value = BASE_MAPPING, method = GET)
    public ResponseEntity<?> followPropertyReference(final RootResourceInformation repoRequest,
            @BackendId Serializable id, final @PathVariable String property, RepresentationModelAssemblers assembler) throws Exception {

        Function<ReferencedProperty, ResponseEntity<?>> handler = (ReferencedProperty prop) -> {
            if (prop.property.isCollectionLike()) {
                var targetType = prop.property.getPersistentEntityTypeInformation().iterator().next();
                var url = this.entityLinks.linkToCollectionResource(targetType.getType()).expand();

                // JPA specific
                var mappedBy = findReverseRelationPropertyName(prop.property);
                if (mappedBy.isEmpty()) {
                    log.warn("Could not find other side of relation {}", prop.property);
                    return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
                }

                var isQueryDslRepository = this.repositories.getRepositoryInformationFor(targetType.getType())
                        .filter(repoMetadata -> QUERY_DSL_PRESENT)
                        .map(RepositoryMetadata::getRepositoryInterface)
                        .filter(QuerydslPredicateExecutor.class::isAssignableFrom)
                        .isPresent();

                var querydslBindingsFactory = this.querydslBindingsFactoryProvider.getIfAvailable();
                if (isQueryDslRepository && querydslBindingsFactory != null) {
                    var querydslBinding = querydslBindingsFactory.createBindingsFor(targetType);
                    var querydslFilter = new QuerydslBindingsInspector(querydslBinding)
                            .findPathBindingFor(mappedBy.get(), targetType.getType());

                    if (querydslFilter.isPresent()) {
                        var filter = querydslFilter.get();
                        var locationUri = URI.create(url.expand().getHref() + "?" + filter + "=" + id);
                        return ResponseEntity.status(HttpStatus.FOUND).location(locationUri).build();
                    } else {
                        log.warn("Querydsl binding for path '{}' type {} not found.",
                                mappedBy.get(), targetType.getType().getName());
                    }
                } else {
                    log.warn("No Querydsl-repository or -bindings found for domain type {}.", targetType.getType());
                }

                // Is a fallback possible to query-methods if target type repo is NOT a querydsl repo ?!
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
            } else if (prop.property.isMap()) {
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
            } else {
                // this is a -to-one type of association, redirect to self link
                return prop.mapValue(target -> this.selfLinkProvider.createSelfLinkFor(prop.getPropertyType(), target))
                        .map(link -> link.getTemplate().expand())
                        .map(uri -> ResponseEntity.status(HttpStatus.FOUND).location(uri).build())
                        .orElse(ResponseEntity.notFound().build());
            }
        };

        return fallbackIfNotImplemented(
                doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.GET),
                () -> delegate.followPropertyReference(repoRequest, id, property, assembler)
        );
    }

    private static Optional<String> findReverseRelationPropertyName(PersistentProperty<?> property) {
        var oneToMany = property.findAnnotation(OneToMany.class);
        if (oneToMany != null) {
            // when this side is the inverse side of a bi-directional relation
            if (!"".equals(oneToMany.mappedBy())) {
                return Optional.of(oneToMany.mappedBy());
            }

            // while it is also possible this is a uni-directional relation using a jointable
            // currently not supported
            return Optional.empty();
        }

        var manyToMany = property.findAnnotation(ManyToMany.class);
        if (manyToMany != null) {
            // in case this is the inverse side
            if (StringUtils.hasText(manyToMany.mappedBy())) {
                return Optional.of(manyToMany.mappedBy());
            }

            // in case this is the primary side, we need to scan the other side to find the field
            // with annotation @ManyToMany(mappedBy=property.getName())
            // (which only exists in for bidirectional relations)
            return Optional.ofNullable(property.getAssociationTargetType())
                    .stream()
                    .flatMap(targetType -> Arrays.stream(targetType.getDeclaredFields()))
                    .filter(field -> {
                        var m2m = field.getAnnotation(ManyToMany.class);
                        return m2m != null && !"".equals(m2m.mappedBy());
                    })
                    .map(Field::getName)
                    .findFirst();
        }

        return Optional.empty();
    }


    @RequestMapping(value = BASE_MAPPING, method = DELETE)
    public ResponseEntity<? extends RepresentationModel<?>> deletePropertyReference(RootResourceInformation repoRequest,
            @BackendId Serializable id, @PathVariable String property) throws Exception {
        return this.delegate.deletePropertyReference(repoRequest, id, property);
    }

    // Differences:
    // * collections:
    // * maps: not implemented
    // * single:
    //      - actually validates the referenced propertyId is valid
    //      - redirects
    @RequestMapping(value = BASE_MAPPING + "/{propertyId}", method = GET)
    public ResponseEntity<?> followPropertyReference(RootResourceInformation repoRequest,
            @BackendId Serializable id, @PathVariable String property, @PathVariable String propertyId,
            RepresentationModelAssemblers assembler)
            throws Exception {

        Function<ReferencedProperty, ResponseEntity<?>> handler = prop -> {

            if (prop.property.isCollectionLike()) {

                // Possible future optimization:
                // Figure out of there is a query-method available via the repo on the other side
                return prop
                        // this is a collection, so we can iterate over it and consume as a stream
                        .mapValue(val -> (Iterable<?>) val)
                        .map(it -> StreamSupport.stream(it.spliterator(), false))
                        .stream().flatMap(stream -> stream)

                        // try to find a subresource with an id matching propertyId
                        .filter(obj -> {

                            IdentifierAccessor accessor = prop.entity.getIdentifierAccessor(obj);
                            return propertyId.equals(Objects.requireNonNull(accessor.getIdentifier()).toString());
                        })
                        .findFirst()

                        // return HTTP 302 with self-link to the subresource
                        .map(linkedResource -> {
                            // found the linked resource with the given propertyId
                            // note that this access pattern is not recommended for larger collections
                            var link = this.selfLinkProvider.createSelfLinkFor(prop.getPropertyType(), linkedResource);
                            return ResponseEntity.status(HttpStatus.FOUND)
                                    .location(link.getTemplate().expand())
                                    .build();
                        })
                        .orElse(ResponseEntity.notFound().build());

            } else if (prop.property.isMap()) {
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
            }

            // single-valued association
            return prop.mapValue(target -> {
                        var idAccessor = prop.entity.getIdentifierAccessor(target);
                        if (propertyId.equals(Objects.requireNonNull(idAccessor.getIdentifier()).toString())) {
                            // the property-id matches
                            return target;
                        }

                        // if the propertyId does not match the linked resource, filter out by returning null
                        return null;
                    })
                    .map(target -> this.selfLinkProvider.createSelfLinkFor(prop.getPropertyType(), target))
                    .map(link -> link.getTemplate().expand())
                    .map(uri -> ResponseEntity.status(HttpStatus.FOUND).location(uri).build())
                    .orElse(ResponseEntity.notFound().build());

        };

        return fallbackIfNotImplemented(
                doWithReferencedProperty(repoRequest, id, property, handler, HttpMethod.GET),
                () -> delegate.followPropertyReference(repoRequest, id, property, propertyId, assembler)
        );
    }

    @RequestMapping(value = BASE_MAPPING, method = GET, produces = TEXT_URI_LIST_VALUE)
    public ResponseEntity<RepresentationModel<?>> followPropertyReferenceCompact(RootResourceInformation repoRequest,
            @BackendId Serializable id, @PathVariable String property, @RequestHeader HttpHeaders requestHeaders,
            RepresentationModelAssemblers assembler) throws Exception {

        return this.delegate.followPropertyReferenceCompact(repoRequest, id, property, requestHeaders, assembler);
    }

    @RequestMapping(value = BASE_MAPPING, method = {PATCH, PUT, POST},
            consumes = {MediaType.APPLICATION_JSON_VALUE, SPRING_DATA_COMPACT_JSON_VALUE, TEXT_URI_LIST_VALUE})
    public ResponseEntity<? extends RepresentationModel<?>> createPropertyReference(
            RootResourceInformation resourceInformation, HttpMethod requestMethod,
            @RequestBody(required = false) CollectionModel<Object> incoming, @BackendId Serializable id,
            @PathVariable String property) throws Exception {

        return this.delegate.createPropertyReference(resourceInformation, requestMethod, incoming, id, property);
    }

    @RequestMapping(value = BASE_MAPPING + "/{propertyId}", method = DELETE)
    public ResponseEntity<RepresentationModel<?>> deletePropertyReferenceId(RootResourceInformation repoRequest,
            @BackendId Serializable backendId, @PathVariable String property, @PathVariable String propertyId)
            throws Exception {

        return this.delegate.deletePropertyReferenceId(repoRequest, backendId, property, propertyId);
    }

    @ExceptionHandler
    public ResponseEntity<Void> handle(
            RepositoryPropertyReferenceController.HttpRequestMethodNotSupportedException exception) {
        return exception.toResponse();
    }

    private ResponseEntity<?> fallbackIfNotImplemented(ResponseEntity<?> currentResponse, ThrowingSupplier<ResponseEntity<?>> fallbackSupplier) throws Exception {
        if(!contentGridRestProperties.isFallbackToDefaultRelationController()) {
            return currentResponse;
        }
        if(currentResponse.getStatusCode() == HttpStatus.NOT_IMPLEMENTED) {
            return fallbackSupplier.get();
        }
        return currentResponse;
    }

    // See RepositoryPropertyReferenceController#doWithReferencedProperty
    private ResponseEntity<?> doWithReferencedProperty(
            RootResourceInformation resourceInformation,
            Serializable id, String propertyPath,
            Function<ReferencedProperty, ResponseEntity<?>> handler,
            HttpMethod method) throws Exception {

        ResourceMetadata metadata = resourceInformation.getResourceMetadata();
        PropertyAwareResourceMapping mapping = metadata.getProperty(propertyPath);

        if (mapping == null || !mapping.isExported()) {
            throw new ResourceNotFoundException();
        }

        PersistentProperty<?> property = mapping.getProperty();
        resourceInformation.verifySupportedMethod(method, property);

        RepositoryInvoker invoker = resourceInformation.getInvoker();
        Optional<Object> domainObj = invoker.invokeFindById(id);

        return domainObj.map(it -> {

                    PersistentPropertyAccessor<?> accessor = property.getOwner().getPropertyAccessor(it);
                    return handler.apply(new ReferencedProperty(property, accessor));
                })
                .orElseThrow(ResourceNotFoundException::new);
    }


    private class ReferencedProperty {

        final PersistentEntity<?, ?> entity;
        final PersistentProperty<?> property;

        @Getter
        private final Class<?> propertyType;
        final PersistentPropertyAccessor<?> accessor;

        private ReferencedProperty(PersistentProperty<?> property, PersistentPropertyAccessor<?> accessor) {

            this.property = property;
            this.accessor = accessor;

            this.propertyType = property.getActualType();
            this.entity = repositories.getPersistentEntity(propertyType);
        }

        public <T> Optional<T> mapValue(Function<Object, T> function) {
            return Optional.ofNullable(accessor.getProperty(property)).map(function);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
