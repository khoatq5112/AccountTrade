package com.group3.accounttrade.dto;

import com.group3.accounttrade.entity.NotificationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Map;

/**
 * DTO for notification preferences.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPreferenceDTO {
    private boolean emailEnabled;
    private boolean digestEnabled;
    private String digestFrequency;
    private Map<String, NotificationPreference.ChannelPreference> categoryPreferences;
    private boolean quietHoursEnabled;
    private LocalTime quietHoursStart;
    private LocalTime quietHoursEnd;
    private String quietHoursTimezone;
}
