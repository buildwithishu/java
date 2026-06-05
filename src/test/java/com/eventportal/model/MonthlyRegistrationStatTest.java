package com.eventportal.model;

import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonthlyRegistrationStatTest {
    @Test
    void constructor_shouldAcceptNonNegativeCounts() {
        MonthlyRegistrationStat stat = new MonthlyRegistrationStat(YearMonth.of(2026, 6), 3);

        assertThat(stat.month()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(stat.registrationCount()).isEqualTo(3);
    }

    @Test
    void constructor_shouldRejectNegativeCounts() {
        assertThatThrownBy(() -> new MonthlyRegistrationStat(YearMonth.of(2026, 6), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("registrationCount");
    }
}
