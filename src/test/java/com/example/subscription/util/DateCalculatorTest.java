package com.example.subscription.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DateCalculatorTest {

    @Test
    void testCalculateEndDate() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        int durationDays = 30;

        // When
        LocalDate endDate = DateCalculator.calculateEndDate(startDate, durationDays);

        // Then
        assertEquals(LocalDate.of(2024, 1, 31), endDate);
    }

    @Test
    void testExtendEndDate() {
        // Given
        LocalDate currentEndDate = LocalDate.of(2024, 1, 31);
        int additionalDays = 30;

        // When
        LocalDate newEndDate = DateCalculator.extendEndDate(currentEndDate, additionalDays);

        // Then
        assertEquals(LocalDate.of(2024, 3, 1), newEndDate);
    }

    @Test
    void testAddDays() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        int days = 7;

        // When
        LocalDate result = DateCalculator.addDays(startDate, days);

        // Then
        assertEquals(LocalDate.of(2024, 1, 8), result);
    }
}

