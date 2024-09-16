package com.contentgrid.spring.data.pagination.cursor;

import com.contentgrid.spring.data.pagination.InvalidPageSizeException;
import com.contentgrid.spring.data.pagination.InvalidPaginationException;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorContext;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorDecodeException;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriComponentsBuilder;

public class HateoasPageableCursorHandlerMethodArgumentResolver extends
        HateoasPageableHandlerMethodArgumentResolver implements
        InitializingBean {

    @NonNull
    private final HateoasSortHandlerMethodArgumentResolver sortResolver;

    @NonNull
    private final CursorCodec cursorCodec;

    @NonNull
    private final Iterable<PageableHandlerMethodArgumentResolverCustomizer> customizers;

    private Pageable fallbackPageable = PageRequest.of(0, 20);

    public HateoasPageableCursorHandlerMethodArgumentResolver(
            HateoasSortHandlerMethodArgumentResolver sortResolver,
            CursorCodec cursorCodec,
            Iterable<PageableHandlerMethodArgumentResolverCustomizer> customizers
    ) {
        super(sortResolver);
        this.sortResolver = sortResolver;
        this.cursorCodec = cursorCodec;
        this.customizers = customizers;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        customizers.forEach(customizer -> customizer.customize(this));
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

        var cursorParameterName = getParameterNameToUse(getPageParameterName(), methodParameter);
        var cursor = webRequest.getParameter(cursorParameterName);
        var sort = sortResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

        try {
            return cursorCodec.decodeCursor(new CursorContext(cursor, pageSize, sort));
        } catch (CursorDecodeException e) {
            throw new InvalidPaginationException(cursorParameterName, e);
        }
    }

    private Optional<Integer> parsePageSize(String pageSizeParam) {
        if (!StringUtils.hasText(pageSizeParam)) {
            return Optional.empty();
        }
        try {
            var size = Integer.parseInt(pageSizeParam);
            if (size <= 0) {
                throw InvalidPageSizeException.mustBePositive(getSizeParameterName());
            }

            if (size < getMaxPageSize()) {
                return Optional.of(size);
            } else {
                return Optional.of(getMaxPageSize());
            }
        } catch (NumberFormatException e) {
            throw new InvalidPageSizeException(getSizeParameterName(), e);
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
