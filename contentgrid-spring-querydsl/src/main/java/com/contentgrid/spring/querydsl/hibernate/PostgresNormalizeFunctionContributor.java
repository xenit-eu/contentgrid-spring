package com.contentgrid.spring.querydsl.hibernate;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;

public class PostgresNormalizeFunctionContributor implements FunctionContributor {

    @Override
    public void contributeFunctions(FunctionContributions functionContributions) {
        var returnType = functionContributions.getTypeConfiguration().getBasicTypeForJavaType(String.class);
        functionContributions.getFunctionRegistry().registerPattern("normalize", "normalize(?1, NFKC)", returnType);
        functionContributions.getFunctionRegistry().registerPattern("contentgrid_normalize", "extensions.contentgrid_prefix_search_normalize(?1)", returnType);
    }
}
