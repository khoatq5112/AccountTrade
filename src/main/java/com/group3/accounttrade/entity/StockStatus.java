package com.group3.accounttrade.entity;

/**
 * Enum representing the stock status of a Post.
 * Stock status is derived from the count of available credentials.
 */
public enum StockStatus {
    /**
     * Post has at least one AVAILABLE credential that can be purchased.
     */
    IN_STOCK,
    
    /**
     * Post has no AVAILABLE credentials - all have been sold.
     */
    OUT_OF_STOCK
}
