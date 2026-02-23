package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "Transaction_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", length = 50, nullable = false, unique = true)
    private String statusName;
}
