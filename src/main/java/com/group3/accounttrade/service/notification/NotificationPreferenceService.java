package com.group3.accounttrade.service.notification;

import com.group3.accounttrade.dto.NotificationPreferenceDTO;
import com.group3.accounttrade.entity.NotificationPreference;
import com.group3.accounttrade.entity.User;
import com.group3.accounttrade.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for managing user notification preferences.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    @Transactional
    public NotificationPreference getOrCreateDefaultPreferences(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }

        return preferenceRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> {
                    NotificationPreference preference = buildDefaultPreferences(user);
                    log.info("Created default notification preferences for user {}", user.getUserId());
                    return preferenceRepository.save(preference);
                });
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceDTO getPreferencesDto(User user) {
        return toDto(getOrCreateDefaultPreferences(user));
    }

    @Transactional
    public NotificationPreferenceDTO updatePreferences(User user, NotificationPreferenceDTO request) {
        NotificationPreference preference = getOrCreateDefaultPreferences(user);
        preference.setEmailEnabled(false);
        preference.setDigestEnabled(request.isDigestEnabled());
        preference.setDigestFrequency(request.getDigestFrequency());
        preference.setQuietHoursEnabled(request.isQuietHoursEnabled());
        preference.setQuietHoursStart(request.getQuietHoursStart());
        preference.setQuietHoursEnd(request.getQuietHoursEnd());
        preference.setQuietHoursTimezone(request.getQuietHoursTimezone());
        preference.setCategoryPreferences(copyCategoryPreferences(request.getCategoryPreferences()));
        return toDto(preferenceRepository.save(preference));
    }

    @Transactional
    public NotificationPreferenceDTO resetPreferences(User user) {
        NotificationPreference preference = preferenceRepository.findByUser_UserId(user.getUserId())
                .orElseGet(() -> NotificationPreference.builder().user(user).build());

        NotificationPreference defaults = buildDefaultPreferences(user);
        preference.setEmailEnabled(defaults.getEmailEnabled());
        preference.setDigestEnabled(defaults.getDigestEnabled());
        preference.setDigestFrequency(defaults.getDigestFrequency());
        preference.setQuietHoursEnabled(defaults.getQuietHoursEnabled());
        preference.setQuietHoursStart(defaults.getQuietHoursStart());
        preference.setQuietHoursEnd(defaults.getQuietHoursEnd());
        preference.setQuietHoursTimezone(defaults.getQuietHoursTimezone());
        preference.setCategoryPreferences(defaults.getCategoryPreferences());
        return toDto(preferenceRepository.save(preference));
    }

    public NotificationPreferenceDTO toDto(NotificationPreference preference) {
        return NotificationPreferenceDTO.builder()
                .emailEnabled(Boolean.TRUE.equals(preference.getEmailEnabled()))
                .digestEnabled(Boolean.TRUE.equals(preference.getDigestEnabled()))
                .digestFrequency(preference.getDigestFrequency())
                .categoryPreferences(copyCategoryPreferences(preference.getCategoryPreferences()))
                .quietHoursEnabled(Boolean.TRUE.equals(preference.getQuietHoursEnabled()))
                .quietHoursStart(preference.getQuietHoursStart())
                .quietHoursEnd(preference.getQuietHoursEnd())
                .quietHoursTimezone(preference.getQuietHoursTimezone())
                .build();
    }

    private NotificationPreference buildDefaultPreferences(User user) {
        return NotificationPreference.builder()
                .user(user)
                .emailEnabled(false)
                .digestEnabled(false)
                .digestFrequency(NotificationPreference.DIGEST_INSTANT)
                .categoryPreferences(defaultCategoryPreferences(user))
                .quietHoursEnabled(false)
                .quietHoursStart(LocalTime.of(22, 0))
                .quietHoursEnd(LocalTime.of(8, 0))
                .quietHoursTimezone("Asia/Saigon")
                .build();
    }

    private Map<String, NotificationPreference.ChannelPreference> defaultCategoryPreferences(User user) {
        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "";
        Map<String, NotificationPreference.ChannelPreference> defaults = new LinkedHashMap<>();

        defaults.put(NotificationPreference.CATEGORY_ORDER, channel(false, true));
        defaults.put(NotificationPreference.CATEGORY_PAYMENT, channel(false, true));
        defaults.put(NotificationPreference.CATEGORY_ESCROW, channel(false, true));
        defaults.put(NotificationPreference.CATEGORY_CREDENTIAL, channel(false, true));
        defaults.put(NotificationPreference.CATEGORY_DISPUTE, channel(false, true));
        defaults.put(NotificationPreference.CATEGORY_SYSTEM, channel(false, true));
        defaults.put(NotificationPreference.CATEGORY_WALLET, channel(false, true));

        if ("Seller".equalsIgnoreCase(roleName)) {
            defaults.put(NotificationPreference.CATEGORY_POST, channel(false, true));
            defaults.put(NotificationPreference.CATEGORY_REVIEW, channel(false, true));
            defaults.put(NotificationPreference.CATEGORY_PROMO, channel(false, true));
        } else if ("Admin".equalsIgnoreCase(roleName)) {
            defaults.put(NotificationPreference.CATEGORY_USER, channel(false, true));
            defaults.put(NotificationPreference.CATEGORY_POST, channel(false, true));
            defaults.put(NotificationPreference.CATEGORY_REVIEW, channel(false, true));
            defaults.put(NotificationPreference.CATEGORY_PROMO, channel(false, false));
        } else {
            defaults.put(NotificationPreference.CATEGORY_PROMO, channel(false, true));
            defaults.put(NotificationPreference.CATEGORY_POST, channel(false, true));
            defaults.put(NotificationPreference.CATEGORY_REVIEW, channel(false, true));
        }

        return defaults;
    }

    private Map<String, NotificationPreference.ChannelPreference> copyCategoryPreferences(
            Map<String, NotificationPreference.ChannelPreference> source) {
        Map<String, NotificationPreference.ChannelPreference> copy = new LinkedHashMap<>();
        if (source == null) {
            return copy;
        }
        source.forEach((key, value) -> copy.put(key, value == null
                ? channel(false, true)
                : channel(false, value.isInApp())));
        return copy;
    }

    private NotificationPreference.ChannelPreference channel(boolean email, boolean inApp) {
        return new NotificationPreference.ChannelPreference(email, inApp);
    }
}
