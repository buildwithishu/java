package com.eventportal.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationUtilTest {
    @Test
    void isValidEmail_shouldAcceptCollegeAddress() {
        assertThat(ValidationUtil.isValidEmail("student@college.edu")).isTrue();
    }

    @Test
    void isValidEmail_shouldRejectInvalidAddress() {
        assertThat(ValidationUtil.isValidEmail("student-at-college")).isFalse();
        assertThat(ValidationUtil.isValidEmail("a".repeat(110) + "@college.edu")).isFalse();
    }

    @Test
    void isValidUsername_shouldEnforceSafeDatabaseLengthAndCharacters() {
        assertThat(ValidationUtil.isValidUsername("student_01")).isTrue();
        assertThat(ValidationUtil.isValidUsername("ab")).isFalse();
        assertThat(ValidationUtil.isValidUsername("student name")).isFalse();
        assertThat(ValidationUtil.isValidUsername("s".repeat(51))).isFalse();
    }

    @Test
    void isStrongPassword_shouldRequireLengthLetterAndDigit() {
        assertThat(ValidationUtil.isStrongPassword("admin123")).isTrue();
        assertThat(ValidationUtil.isStrongPassword("password")).isFalse();
        assertThat(ValidationUtil.isStrongPassword("12345678")).isFalse();
        assertThat(ValidationUtil.isStrongPassword("a1" + "x".repeat(71))).isFalse();
    }

    @Test
    void isValidTimeRange_shouldRequireEndAfterStart() {
        assertThat(ValidationUtil.isValidTimeRange(LocalTime.of(10, 0), LocalTime.of(11, 0))).isTrue();
        assertThat(ValidationUtil.isValidTimeRange(LocalTime.of(11, 0), LocalTime.of(10, 0))).isFalse();
    }

    @Test
    void isFutureOrToday_shouldRejectPastDates() {
        assertThat(ValidationUtil.isFutureOrToday(LocalDate.now())).isTrue();
        assertThat(ValidationUtil.isFutureOrToday(LocalDate.now().minusDays(1))).isFalse();
    }
}
