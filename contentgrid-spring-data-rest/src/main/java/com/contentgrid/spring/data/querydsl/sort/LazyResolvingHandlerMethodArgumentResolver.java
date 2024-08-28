package com.contentgrid.spring.data.querydsl.sort;

import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.data.util.Lazy;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@RequiredArgsConstructor
public class LazyResolvingHandlerMethodArgumentResolver<T extends HandlerMethodArgumentResolver> implements HandlerMethodArgumentResolver {
    private final Lazy<T> delegate;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return delegate.get().supportsParameter(parameter);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return delegate.get().resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }
}
