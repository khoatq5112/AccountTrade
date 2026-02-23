package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "Post_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;

    @Column(name = "status_name", length = 50, nullable = false, unique = true)
    private String statusName;

    @Column(name = "description")
    private String description;
}
