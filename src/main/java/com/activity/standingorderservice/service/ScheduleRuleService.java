package com.activity.standingorderservice.service;

import org.springframework.stereotype.Service;

@Service
public class ScheduleRuleService {

    public String calculateNextOccurrence() {
        return "2026-10-25T09:00:00+08:00";
    }
}