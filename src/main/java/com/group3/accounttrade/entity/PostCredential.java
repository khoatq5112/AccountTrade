package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "Post_Credentials")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "credential_id")
    private Integer credentialId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    @ToString.Exclude
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_status_id", nullable = false)
    @ToString.Exclude
    private CredentialStatus credentialStatus;

    @OneToMany(mappedBy = "credential", fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private java.util.List<CredentialAssignment> assignments = new java.util.ArrayList<>();

    @Column(name = "account_username", nullable = false)
    private String accountUsername;

    @Column(name = "account_password", nullable = false)
    private String accountPassword;

    @Lob
    @Column(name = "security_notes", columnDefinition = "TEXT")
    private String securityNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private String soldOrderNumber;

    @Transient
    public boolean isAvailable() {
        return credentialStatus != null && CredentialStatus.AVAILABLE.equals(credentialStatus.getStatusName());
    }

    @Transient
    public boolean isSold() {
        return credentialStatus != null && CredentialStatus.SOLD.equals(credentialStatus.getStatusName());
    }

    @Transient
    public boolean isHolding() {
        return credentialStatus != null && CredentialStatus.HOLDING.equals(credentialStatus.getStatusName());
    }

    @Transient
    public boolean isHidden() {
        return credentialStatus != null && CredentialStatus.HIDDEN.equals(credentialStatus.getStatusName());
    }
}
