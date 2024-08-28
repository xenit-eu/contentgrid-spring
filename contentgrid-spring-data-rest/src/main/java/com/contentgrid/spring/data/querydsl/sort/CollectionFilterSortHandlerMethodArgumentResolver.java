package com.contentgrid.spring.data.querydsl.sort;

import com.contentgrid.spring.querydsl.mapping.CollectionFilter;
import com.contentgrid.spring.querydsl.mapping.CollectionFiltersMapping;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.OrderSpecifier.NullHandling;
import com.querydsl.core.types.dsl.ComparableExpression;
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
        var resourceMetadata = resourceMetadataHandlerMethodArgumentResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);

        var collectionFilters = collectionFiltersMapping.forDomainType(resourceMetadata.getDomainType());

        List<Order> processedOrders = new ArrayList<>();
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

        for (Order order : originalSort) {
            collectionFilters.named(order.getProperty())
                    .filter(this::isSortableFilterSpec)
                    .ifPresent(collectionFilter -> {
                        orderSpecifiers.add(new OrderSpecifier<>(
                                convertDirection(order.getDirection()),
                                (ComparableExpression<?>)collectionFilter.getPath(),
                                convertNullHandling(order.getNullHandling())
                        ));
                        processedOrders.add(order);
                    });

        }

        return new QSortWithOriginalSort(
                Sort.by(processedOrders),
                orderSpecifiers
        );
    }

    private boolean isSortableFilterSpec(CollectionFilter<?> collectionFilter) {
        var path = collectionFilter.getPath();
        if(!(path instanceof ComparableExpression<?>)) {
            return false;
        }

        while(path.getMetadata().getParent() != null) {
            if(path instanceof EntityPath<?>) {
                // This goes across a relation; we don't want to order across relations
                // Note that the root path always is an EntityPath, but it will never be reached due to the condition in the while-loop
                return false;
            }
            path = path.getMetadata().getParent();
        }
        return true;
    }

    private static com.querydsl.core.types.Order convertDirection(Direction direction) {
        return switch(direction) {
            case ASC -> com.querydsl.core.types.Order.ASC;
            case DESC -> com.querydsl.core.types.Order.DESC;
        };
    }

    private static NullHandling convertNullHandling(Sort.NullHandling nullHandling) {
        return switch (nullHandling) {
            case NATIVE -> NullHandling.Default;
            case NULLS_FIRST -> NullHandling.NullsFirst;
            case NULLS_LAST -> NullHandling.NullsLast;
        };
    }
}
