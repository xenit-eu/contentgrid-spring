package com.contentgrid.spring.data.rest.links;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.hateoas.server.core.EmbeddedWrapper;
import org.springframework.hateoas.server.core.EmbeddedWrappers;
import org.springframework.lang.Nullable;

/**
 * Changes the link-relation for items in a collection to 'item'.
 * <p>
 * This processor must run with highest precedence, so it executes before any other resource processors that may add
 * additional {@link EmbeddedWrapper}s to the resource.
 */
@RequiredArgsConstructor
public class SpringDataEmbeddedItemResourceProcessor implements RepresentationModelProcessor<CollectionModel<?>>,
        Ordered {

    private final EmbeddedWrappers wrappers = new EmbeddedWrappers(true);

    @Override
    public CollectionModel<?> process(CollectionModel<?> model) {
        var content = model.getContent();

        var wrapper = findEmptyWrapper(content)
                .<EmbeddedWrapper>map(w -> new ForcedLinkRelEmbeddedWrapper(w, IanaLinkRelations.ITEM))
                .orElseGet(() -> {
                    if (hasWrappers(content)) {
                        throw new UnsupportedOperationException(
                                "Model is already wrapped; cannot identify the primary collection to set up the 'item' collection");
                    }
                    return wrappers.wrap(content, IanaLinkRelations.ITEM);
                });

        return createModelCopy(model, List.of(wrapper));
    }

    private CollectionModel<?> createModelCopy(CollectionModel<?> model, Collection<?> content) {
        if (model instanceof PagedModel<?> pagedModel) {
            return PagedModel.of(content, pagedModel.getMetadata(), model.getLinks());
        } else {
            return CollectionModel.of(content, model.getLinks());
        }
    }

    private boolean hasWrappers(Collection<?> collection) {
        for (Object item : collection) {
            if (item instanceof EmbeddedWrapper) {
                return true;
            }
        }
        return false;
    }

    private Optional<EmbeddedWrapper> findEmptyWrapper(Collection<?> collection) {
        if (collection.size() != 1) {
            return Optional.empty();
        }
        if (collection.stream().findAny().orElseThrow() instanceof EmbeddedWrapper embeddedWrapper) {
            if (embeddedWrapper.getValue() instanceof Collection<?> embeddedCollection) {
                if (embeddedCollection.isEmpty()) {
                    return Optional.of(embeddedWrapper);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @RequiredArgsConstructor
    private static class ForcedLinkRelEmbeddedWrapper implements EmbeddedWrapper {

        private final EmbeddedWrapper delegate;
        private final LinkRelation linkRelation;

        @Override
        public Optional<LinkRelation> getRel() {
            return Optional.of(linkRelation);
        }

        @Override
        public boolean hasRel(LinkRelation rel) {
            return linkRelation.isSameAs(rel);
        }

        @Override
        public boolean isCollectionValue() {
            return delegate.isCollectionValue();
        }

        @Override
        public Object getValue() {
            return delegate.getValue();
        }

        @Override
        public Class<?> getRelTargetType() {
            return delegate.getRelTargetType();
        }
    }
}
