package com.eventportal.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeUtilTest {
    @Test
    void formatDateTime_shouldRenderDateAndTime() {
        OffsetDateTime dateTime = OffsetDateTime.of(2026, 6, 5, 14, 30, 0, 0, ZoneOffset.UTC);

        assertThat(DateTimeUtil.formatDateTime(dateTime)).isEqualTo("05 Jun 2026, 02:30 PM");
    }

    @Test
    void formatters_shouldReturnEmptyStringForNulls() {
        assertThat(DateTimeUtil.formatDate(null)).isEmpty();
        assertThat(DateTimeUtil.formatTime(null)).isEmpty();
        assertThat(DateTimeUtil.formatDateTime(null)).isEmpty();
    }

    @Test
    void formatDateAndTime_shouldUseConsistentDisplayPatterns() {
        assertThat(DateTimeUtil.formatDate(LocalDate.of(2026, 6, 5))).isEqualTo("05 Jun 2026");
        assertThat(DateTimeUtil.formatTime(LocalTime.of(9, 15))).isEqualTo("09:15 AM");
    }
}
