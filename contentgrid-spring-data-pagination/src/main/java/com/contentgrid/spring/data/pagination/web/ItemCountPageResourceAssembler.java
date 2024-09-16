package com.contentgrid.spring.data.pagination.web;

import com.contentgrid.spring.data.pagination.ItemCountPage;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;

public class ItemCountPageResourceAssembler<T> extends PagedResourcesAssembler<T> {

    private final Lazy<CursorCodec> lazyCursorCodec;

    public ItemCountPageResourceAssembler(
            HateoasPageableHandlerMethodArgumentResolver resolver,
            Lazy<CursorCodec> lazyCursorCodec
    ) {
        super(resolver, null);
        this.lazyCursorCodec = lazyCursorCodec;
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
            return new ItemCountPageMetadata(
                    metadata,
                    itemCountPage.getTotalItemCount(),
                    createCursorMetadata(page)
            );
        }
        return metadata;
    }

    private CursorPageMetadata createCursorMetadata(Page<?> page) {
        return lazyCursorCodec.getOptional().map(cursorCodec -> {
            var prevCursor = page.hasPrevious() ? cursorCodec.encodeCursor(page.previousPageable()).cursor() : null;
            var nextCursor = page.hasNext() ? cursorCodec.encodeCursor(page.nextPageable()).cursor() : null;
            return new CursorPageMetadata(prevCursor, nextCursor);
        }).orElseGet(() -> new CursorPageMetadata(null, null));
    }

}
