package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", unique = true)
    private Post post;

    @Column(name = "account_username", nullable = false)
    private String accountUsername;

    @Column(name = "account_password", nullable = false)
    private String accountPassword;

    @Column(name = "security_notes", columnDefinition = "TEXT")
    private String securityNotes;
}
