package com.ewb.standingorder.service;

import org.springframework.stereotype.Service;

@Service
public class ScheduleRuleService {

    public String calculateNextOccurrence(Integer dayOfMonth, String executionTime, String timeZone) {
        if (dayOfMonth == null) {
            dayOfMonth = 25;
        }
        if (executionTime == null || executionTime.isBlank()) {
            executionTime = "09:00:00";
        } else if (executionTime.length() == 5) {
            executionTime = executionTime + ":00";
        }
        String dayStr = String.format("%02d", dayOfMonth);
        return "2026-10-" + dayStr + "T" + executionTime + "+08:00";
    }
}
