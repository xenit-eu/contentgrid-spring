package com.contentgrid.spring.data.pagination.jpa.hibernate;

import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolver;

public class CountExplainDialectResolver implements DialectResolver {

    private static final DialectResolver delegate = new StandardDialectResolver();

    @Override
    public Dialect resolveDialect(DialectResolutionInfo info) {
        var dialect = delegate.resolveDialect(info);
        if (dialect instanceof PostgreSQLDialect) {
            return new CountExplainPostgreSQLDialect();
        }
        return null;
    }
}
