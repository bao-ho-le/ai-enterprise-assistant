package com.enterprise.aiassistant.backend.ai.usage.dto;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;

import com.enterprise.aiassistant.backend.ai.usage.enums.AIUsageStatus;

import lombok.Data;

@Data
public class AIUsageLogFilterRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime toDate;

    private String featureType;

    private String model;

    private AIUsageStatus status;
}