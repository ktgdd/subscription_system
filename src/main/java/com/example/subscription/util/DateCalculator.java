package com.example.subscription.util;

import java.time.LocalDate;

public class DateCalculator {
    
    public static LocalDate addDays(LocalDate startDate, int days) {
        return startDate.plusDays(days);
    }
    
    public static LocalDate calculateEndDate(LocalDate startDate, int durationDays) {
        return addDays(startDate, durationDays);
    }
    
    public static LocalDate extendEndDate(LocalDate currentEndDate, int additionalDays) {
        return addDays(currentEndDate, additionalDays);
    }
}

