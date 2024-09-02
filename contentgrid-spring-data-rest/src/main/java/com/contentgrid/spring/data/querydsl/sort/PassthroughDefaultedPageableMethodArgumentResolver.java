package com.contentgrid.spring.data.querydsl.sort;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.data.rest.webmvc.support.DefaultedPageable;
import org.springframework.data.util.Lazy;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
public class PassthroughDefaultedPageableMethodArgumentResolver implements HandlerMethodArgumentResolver {
    private final Lazy<PageableHandlerMethodArgumentResolver> delegate;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return DefaultedPageable.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        var pageable = delegate.get().resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        return new DefaultedPageable(pageable, delegate.get().isFallbackPageable(pageable));
    }
}
