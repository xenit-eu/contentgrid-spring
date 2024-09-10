package com.contentgrid.spring.data.pagination.jpa.hibernate;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.dialect.PostgreSQLDialect;

public class CountExplainWrappingDialectWrapper extends PostgreSQLDialect {

    public static final String COUNT_EXPLAIN_HINT = "count_explain_estimate";

    @Override
    public String getQueryHintString(String query, List<String> hintList) {
        var hints = new ArrayList<>(hintList);
        var countExplain = hints.remove(COUNT_EXPLAIN_HINT);
        var queryWithHints = super.getQueryHintString(query, hints);

        if (countExplain) {
            return "EXPLAIN (FORMAT JSON) " + queryWithHints;
        }

        return queryWithHints;
    }
}
