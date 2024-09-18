package com.contentgrid.spring.data.pagination.cursor;

import com.contentgrid.spring.data.pagination.InvalidPageSizeException;
import com.contentgrid.spring.data.pagination.InvalidPaginationException;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorContext;
import com.contentgrid.spring.data.pagination.cursor.CursorCodec.CursorDecodeException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import lombok.NonNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

public class HateoasPageableCursorHandlerMethodArgumentResolver extends
        HateoasPageableHandlerMethodArgumentResolver implements
        InitializingBean,
        CursorEncoder {

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
        var pageSizeParameterName = getParameterNameToUse(getSizeParameterName(), methodParameter);
        var pageSize = parsePageSize(webRequest.getParameter(pageSizeParameterName))
                .orElseGet(() -> getDefaultPageSize(methodParameter));

        var cursorParameterName = getParameterNameToUse(getPageParameterName(), methodParameter);
        var cursor = webRequest.getParameter(cursorParameterName);
        var sort = sortResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);

        var uriComponentsBuilder = Optional.ofNullable(webRequest.getNativeRequest(HttpServletRequest.class))
                .map(ServletUriComponentsBuilder::fromRequest)
                .orElseGet(ServletUriComponentsBuilder::fromCurrentRequest);

        // Clear out cursor, page size and sort parameter
        uriComponentsBuilder.replaceQueryParam(cursorParameterName);
        uriComponentsBuilder.replaceQueryParam(pageSizeParameterName);
        sortResolver.enhance(uriComponentsBuilder, methodParameter, Sort.unsorted());

        try {
            return cursorCodec.decodeCursor(new CursorContext(cursor, pageSize, sort), uriComponentsBuilder.build());
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
    public void enhance(UriComponentsBuilder builder, @Nullable MethodParameter parameter, Object value) {
        if (!(value instanceof Pageable pageable)) {
            return;
        }

        if (pageable.isUnpaged()) {
            return;
        }

        var cursorPropertyName = getParameterNameToUse(getPageParameterName(), parameter);
        var pageSizePropertyName = getParameterNameToUse(getSizeParameterName(), parameter);

        // Clear out cursor, page size & sort
        builder.replaceQueryParam(cursorPropertyName);
        builder.replaceQueryParam(pageSizePropertyName);
        sortResolver.enhance(builder, parameter, Sort.unsorted());

        var context = cursorCodec.encodeCursor(pageable, builder.build());

        if (context.pageSize() != getDefaultPageSize(parameter)) {
            builder.replaceQueryParam(pageSizePropertyName, context.pageSize());
        }
        if (context.cursor() != null) {
            builder.replaceQueryParam(cursorPropertyName, context.cursor());
        }

        sortResolver.enhance(builder, parameter, context.sort());
    }

    @Override
    public String encodeCursor(Pageable pageable, String referenceUrl) {
        var componentsBuilder = UriComponentsBuilder.fromHttpUrl(referenceUrl);
        enhance(componentsBuilder, null, pageable);

        return componentsBuilder.build().getQueryParams()
                .getFirst(getParameterNameToUse(getPageParameterName(), null));
    }

    @Override
    public void setFallbackPageable(Pageable fallbackPageable) {
        this.fallbackPageable = fallbackPageable;
        super.setFallbackPageable(fallbackPageable);
    }

    private int getDefaultPageSize(@Nullable MethodParameter methodParameter) {
        if (methodParameter == null) {
            return fallbackPageable.getPageSize();
        }
        var defaults = MergedAnnotations.from(methodParameter.getParameterAnnotations()).get(PageableDefault.class);

        if (defaults.isPresent()) {
            return defaults.getInt("size");
        } else {
            return fallbackPageable.getPageSize();
        }
    }
}
