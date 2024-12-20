package com.contentgrid.spring.data.rest.webmvc.blueprint;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.lang.Nullable;

public enum DataType {
    STRING,
    LONG,
    DOUBLE,
    BOOLEAN,
    DATETIME,
    OBJECT;

    private static final Collection<Class<?>> STRING_TYPES = List.of(String.class, UUID.class);
    private static final Collection<Class<?>> LONG_TYPES = List.of(int.class, long.class, short.class,
            Integer.class, Long.class, Short.class);
    private static final Collection<Class<?>> DOUBLE_TYPES = List.of(float.class, double.class,
            Float.class, Double.class, BigDecimal.class);
    private static final Collection<Class<?>> BOOLEAN_TYPES = List.of(boolean.class, Boolean.class);
    private static final Collection<Class<?>> DATETIME_TYPES = List.of(Instant.class, LocalDateTime.class,
            OffsetDateTime.class, ZonedDateTime.class);

    @Nullable
    public static DataType from(Class<?> type) {

         if (STRING_TYPES.contains(type)) {
             return STRING;
         }

         if (LONG_TYPES.contains(type)) {
             return LONG;
         }

         if (DOUBLE_TYPES.contains(type)) {
             return DOUBLE;
         }

         if (BOOLEAN_TYPES.contains(type)) {
             return BOOLEAN;
         }

         if (DATETIME_TYPES.contains(type)) {
             return DATETIME;
         }

         return null;
    }
}
