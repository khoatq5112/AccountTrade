package com.group3.accounttrade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Entity representing user notification preferences.
 * Stores settings for email, in-app notifications, quiet hours, etc.
 */
@Data
@Entity
@Table(name = "notification_preferences")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Global email notification toggle.
     */
    @Column(name = "email_enabled")
    @Builder.Default
    private Boolean emailEnabled = false;

    /**
     * Whether to receive digest emails instead of instant.
     */
    @Column(name = "digest_enabled")
    @Builder.Default
    private Boolean digestEnabled = false;

    /**
     * Digest frequency: INSTANT, HOURLY, DAILY, WEEKLY.
     */
    @Column(name = "digest_frequency", length = 20)
    @Builder.Default
    private String digestFrequency = "INSTANT";

    /**
     * Category-specific preferences stored as JSON.
     * Example: {"ORDER": {"email": true, "inApp": true}, "DISPUTE": {"email": true, "inApp": true}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "category_preferences", columnDefinition = "JSON")
    @Builder.Default
    private Map<String, ChannelPreference> categoryPreferences = new HashMap<>();

    /**
     * Whether quiet hours are enabled.
     */
    @Column(name = "quiet_hours_enabled")
    @Builder.Default
    private Boolean quietHoursEnabled = false;

    /**
     * Start time for quiet hours (no email notifications).
     */
    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    /**
     * End time for quiet hours.
     */
    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    /**
     * Timezone for quiet hours calculation.
     */
    @Column(name = "quiet_hours_timezone", length = 50)
    @Builder.Default
    private String quietHoursTimezone = "Asia/Saigon";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Nested class for channel-specific preferences per category.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChannelPreference {
        private boolean email;
        private boolean inApp;
    }

    // Digest frequency constants
    public static final String DIGEST_INSTANT = "INSTANT";
    public static final String DIGEST_HOURLY = "HOURLY";
    public static final String DIGEST_DAILY = "DAILY";
    public static final String DIGEST_WEEKLY = "WEEKLY";

    // Category constants
    public static final String CATEGORY_ORDER = "ORDER";
    public static final String CATEGORY_PAYMENT = "PAYMENT";
    public static final String CATEGORY_ESCROW = "ESCROW";
    public static final String CATEGORY_CREDENTIAL = "CREDENTIAL";
    public static final String CATEGORY_DISPUTE = "DISPUTE";
    public static final String CATEGORY_POST = "POST";
    public static final String CATEGORY_WALLET = "WALLET";
    public static final String CATEGORY_REVIEW = "REVIEW";
    public static final String CATEGORY_PROMO = "PROMO";
    public static final String CATEGORY_SYSTEM = "SYSTEM";
    public static final String CATEGORY_USER = "USER";
}
