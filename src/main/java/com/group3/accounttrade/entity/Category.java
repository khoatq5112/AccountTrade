package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing a single-level product category.
 * Parent-child hierarchy has been removed for simplicity.
 */
@Data
@Entity
@Table(name = "Categories")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer categoryId;

    @Column(name = "category_name", nullable = false, unique = true)
    private String categoryName;

    @Column(name = "category_icon")
    private String categoryIcon;

    @Column(name = "display_order")
    private Integer displayOrder;
}
