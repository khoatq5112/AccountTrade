package com.group3.accounttrade.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

class EntityVersionDefaultsTest {

    @Test
    void builderLeavesOptimisticLockVersionsUnsetForNewEntities() {
        assertNull(Order.builder().build().getVersion());
        assertNull(OrderItem.builder().build().getVersion());
        assertNull(Payment.builder().build().getVersion());
        assertNull(Escrow.builder().build().getVersion());
        assertNull(CredentialAssignment.builder().build().getVersion());
        assertNull(Dispute.builder().build().getVersion());
        assertNull(RefundRequest.builder().build().getVersion());
    }
}
