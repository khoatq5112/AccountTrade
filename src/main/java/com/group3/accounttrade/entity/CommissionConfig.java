package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "commission_configs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "global_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal globalRatePercent;

    @Column(name = "min_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal minRatePercent;

    @Column(name = "max_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxRatePercent;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;
}
