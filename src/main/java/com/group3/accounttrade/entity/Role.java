package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "Roles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;

    @Column(name = "role_name", length = 50, nullable = false, unique = true)
    private String roleName;

    @Column(name = "permissions", columnDefinition = "TEXT")
    private String permissions;
}
