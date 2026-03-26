package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "wallet_transactions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    public static final String TYPE_TOP_UP = "TOP_UP";
    public static final String TYPE_PURCHASE = "PURCHASE";
    public static final String TYPE_REFUND = "REFUND";
    public static final String TYPE_SELLER_EARNING = "SELLER_EARNING";
    public static final String TYPE_PLATFORM_COMMISSION = "PLATFORM_COMMISSION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Column(name = "type", nullable = false, length = 30)
    private String type;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
