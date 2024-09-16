package com.contentgrid.spring.data.pagination.web;

import com.contentgrid.spring.data.pagination.ItemCountPage;
import org.springframework.data.domain.Page;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ItemCountPageResourceAssembler<T> extends PagedResourcesAssembler<T> {

    public ItemCountPageResourceAssembler(
            HateoasPageableHandlerMethodArgumentResolver resolver
    ) {
        super(resolver, null);
    }

    @Override
    public PagedModel<?> toEmptyModel(Page<?> page, Class<?> type) {
        return maybeReplacePageModel(page, super.toEmptyModel(page, type));
    }

    @Override
    public PagedModel<?> toEmptyModel(Page<?> page, Class<?> type, Link link) {
        return maybeReplacePageModel(page, super.toEmptyModel(page, type, link));
    }

    @Override
    public <R extends RepresentationModel<?>> PagedModel<R> toModel(Page<T> page,
            RepresentationModelAssembler<T, R> assembler) {
        return maybeReplacePageModel(page, super.toModel(page, assembler));
    }

    @Override
    public <R extends RepresentationModel<?>> PagedModel<R> toModel(Page<T> page,
            RepresentationModelAssembler<T, R> assembler, Link link) {
        return maybeReplacePageModel(page, super.toModel(page, assembler, link));
    }

    private <R> PagedModel<R> maybeReplacePageModel(Page<?> page, PagedModel<R> model) {
        var newModel = PagedModel.of(model.getContent(), maybeWrapMetadata(page, model.getMetadata()));
        newModel.add(model.getLinks().stream()
                // 'last' link can not be calculated when the count is estimated.
                // Additionally, being able to navigate to the last page is not an action that is expected to be useful,
                // as a similar result can be reached by inverting the sorting
                .filter(link -> !link.hasRel(IanaLinkRelations.LAST))
                .toList());
        return newModel;
    }

    private PageMetadata maybeWrapMetadata(Page<?> page, PageMetadata metadata) {
        if (page instanceof ItemCountPage<?> itemCountPage) {
            return new ItemCountPageMetadata(metadata, itemCountPage.getTotalItemCount());
        }
        return metadata;
    }

}
