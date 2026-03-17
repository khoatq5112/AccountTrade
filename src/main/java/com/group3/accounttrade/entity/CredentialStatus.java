package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing the status of a PostCredential.
 * Each credential can have one of these statuses: Available, Holding, Sold, Hidden.
 */
@Data
@Entity
@Table(name = "Credential_Statuses")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CredentialStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Integer statusId;
    
    @Column(name = "status_name", nullable = false, unique = true)
    private String statusName;
    
    @Column(name = "description")
    private String description;
    
    // Constants for common status names
    public static final String AVAILABLE = "Available";
    public static final String HOLDING = "Holding";
    public static final String SOLD = "Sold";
    public static final String HIDDEN = "Hidden";
}
