package com.contentgrid.spring.data.pagination.cursor;

import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorContext;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableDefault;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriComponentsBuilder;

public class HateoasPageableCursorHandlerMethodArgumentResolver extends HateoasPageableHandlerMethodArgumentResolver {

    private final HateoasSortHandlerMethodArgumentResolver sortResolver;
    private final CursorCodec cursorCodec;
    private Pageable fallbackPageable = PageRequest.of(0, 20);

    public HateoasPageableCursorHandlerMethodArgumentResolver(
            HateoasSortHandlerMethodArgumentResolver sortResolver,
            CursorCodec cursorCodec
    ) {
        super(sortResolver);
        this.sortResolver = sortResolver;
        this.cursorCodec = cursorCodec;
    }

    @Override
    public Pageable resolveArgument(MethodParameter methodParameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var defaults = MergedAnnotations.from(methodParameter.getParameterAnnotations()).get(PageableDefault.class);
        var pageSize = parsePageSize(
                webRequest.getParameter(getParameterNameToUse(getSizeParameterName(), methodParameter)))
                .orElseGet(() -> {
                    if (defaults.isPresent()) {
                        return defaults.getInt("size");
                    } else {
                        return fallbackPageable.getPageSize();
                    }
                });

        var cursor = webRequest.getParameter(getParameterNameToUse(getPageParameterName(), methodParameter));
        var sort = sortResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

        return cursorCodec.decodeCursor(new CursorContext(cursor, pageSize, sort));
    }

    private Optional<Integer> parsePageSize(String pageSizeParam) {
        if (!StringUtils.hasText(pageSizeParam)) {
            return Optional.empty();
        }
        try {
            var size = Integer.parseInt(pageSizeParam);
            if (size < 0) {
                return Optional.empty();
            }

            if (size < getMaxPageSize()) {
                return Optional.of(size);
            } else {
                return Optional.of(getMaxPageSize());
            }
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }


    @Override
    public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {
        if (!(value instanceof Pageable pageable)) {
            return;
        }

        if (pageable.isUnpaged()) {
            return;
        }

        var cursorPropertyName = getParameterNameToUse(getPageParameterName(), parameter);
        var pageSizePropertyName = getParameterNameToUse(getSizeParameterName(), parameter);

        var context = cursorCodec.encodeCursor(pageable);

        builder.replaceQueryParam(pageSizePropertyName, context.pageSize());
        builder.replaceQueryParam(cursorPropertyName, context.cursor());

        sortResolver.enhance(builder, parameter, context.sort());
    }

    @Override
    public void setFallbackPageable(Pageable fallbackPageable) {
        this.fallbackPageable = fallbackPageable;
        super.setFallbackPageable(fallbackPageable);
    }
}
