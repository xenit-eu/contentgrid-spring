package com.contentgrid.spring.querydsl.predicate;

/**
 * Compares strings in a case-insensitive way.
 *
 * This predicate only supports strings and can not be used with other types.
 * @deprecated Use {@link Text.EqualsIgnoreCase} instead.
 */
@Deprecated(since = "v0.15.2", forRemoval = true)
public class EqualsIgnoreCase extends Text.EqualsIgnoreCase {
}
