package com.contentgrid.spring.data.pagination;

import lombok.Value;

/**
 * Information about the number of items in a {@link ItemCountPage}
 */
public sealed interface ItemCount {

    /**
     * A verified, exact count of the number of items
     *
     * @param count Exact number of items
     */
    static ItemCount exact(long count) {
        return new ItemCountImpl(count, false);
    }

    /**
     * An estimate of the number of items
     *
     * @param estimate Estimate of the number of items
     */
    static ItemCount estimated(long estimate) {
        return new ItemCountImpl(estimate, true);
    }

    /**
     * A completely unknown number of items
     * <p>
     * An unknown number of items will take any additional information about the actual number of items as a rough
     * estimate
     */
    static ItemCount unknown() {
        return UnknownItemCount.INSTANCE;
    }

    /**
     * @return The number of items that was counted, either exact or estimated
     */
    long count();

    /**
     * @return Whether the {@link #count()} is an estimate, or is exact
     */
    boolean isEstimated();

    /**
     * Forces the count result to be at least a minimal value
     * <p>
     * If the count is changed because of the minimal value, the quality of the result will always be an estimate
     */
    ItemCount orMinimally(long minimalValue);

    /**
     * Forces the count result to be at most a maximal value
     * <p>
     * If the count is changed because of the maximal value, the quality of the result will always be an estimate
     */
    ItemCount orMaximally(long maximalValue);
}

@Value
class ItemCountImpl implements ItemCount {

    long count;
    boolean estimate;

    @Override
    public long count() {
        return count;
    }

    @Override
    public boolean isEstimated() {
        return estimate;
    }

    public ItemCount orMinimally(long minimalValue) {
        if (count < minimalValue) {
            return ItemCount.estimated(minimalValue);
        }
        return this;
    }

    public ItemCount orMaximally(long maximalValue) {
        if (count > maximalValue) {
            return ItemCount.estimated(maximalValue);
        }
        return this;
    }
}

enum UnknownItemCount implements ItemCount {
    INSTANCE;

    @Override
    public long count() {
        return 0;
    }

    @Override
    public boolean isEstimated() {
        return true;
    }

    @Override
    public ItemCount orMinimally(long minimalValue) {
        return ItemCount.estimated(minimalValue);
    }

    @Override
    public ItemCount orMaximally(long maximalValue) {
        return ItemCount.estimated(maximalValue);
    }
}
