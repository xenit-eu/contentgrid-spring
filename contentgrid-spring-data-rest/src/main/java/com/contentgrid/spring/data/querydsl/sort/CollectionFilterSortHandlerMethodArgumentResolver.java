package com.contentgrid.spring.data.querydsl.sort;

import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.querydsl.core.types.OrderSpecifier;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.rest.webmvc.config.ResourceMetadataHandlerMethodArgumentResolver;
import org.springframework.data.web.HateoasSortHandlerMethodArgumentResolver;
import org.springframework.data.web.SortArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
public class CollectionFilterSortHandlerMethodArgumentResolver extends
        HateoasSortHandlerMethodArgumentResolver implements SortArgumentResolver {

    private final SortArgumentResolver delegate;
    private final CollectionFiltersMapping collectionFiltersMapping;
    private final ResourceMetadataHandlerMethodArgumentResolver resourceMetadataHandlerMethodArgumentResolver;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return delegate.supportsParameter(parameter);
    }

    @Override
    @SneakyThrows
    public Sort resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        var originalSort = delegate.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        var resourceMetadata = resourceMetadataHandlerMethodArgumentResolver.resolveArgument(parameter, mavContainer,
                webRequest, binderFactory);
        if (resourceMetadata == null) {
            return originalSort;
        }

        var sortFilters = collectionFiltersMapping.forDomainType(resourceMetadata.getDomainType()).forSorting();

        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        for (Order order : originalSort) {
            var orderSpecifier = sortFilters
                    .named(order.getProperty())
                    .flatMap(cf -> cf.createOrderSpecifier(convertDirection(order.getDirection())))
                    .orElseThrow(() -> new UnsupportedSortPropertyException(order));

            orderSpecifiers.add(orderSpecifier);
        }

        return new QSortWithOriginalSort(
                originalSort,
                orderSpecifiers
        );
    }


    @Override
    public void enhance(UriComponentsBuilder builder, MethodParameter parameter, Object value) {
        if (value instanceof QSortWithOriginalSort qSortWithOriginalSort) {
            super.enhance(builder, parameter, qSortWithOriginalSort.getOriginalSort());
        } else {
            super.enhance(builder, parameter, value);
        }
    }

    private static com.querydsl.core.types.Order convertDirection(Direction direction) {
        return switch (direction) {
            case ASC -> com.querydsl.core.types.Order.ASC;
            case DESC -> com.querydsl.core.types.Order.DESC;
        };
    }

}
