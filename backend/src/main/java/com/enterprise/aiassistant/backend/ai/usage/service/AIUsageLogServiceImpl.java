package com.enterprise.aiassistant.backend.ai.usage.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.helper.AiUsageHelper;
import com.enterprise.aiassistant.backend.ai.usage.mapper.AIUsageLogMapper;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIUsageLogServiceImpl implements AIUsageLogService {

    private final AIUsageLogMapper mapper;
    private final AIUsageLogRepository repository;
    private final AiUsageHelper aiUsageHelper;

    @Override
    @Transactional
    public void logAiUsage(AIUsageLogRequest request) {
        aiUsageHelper.validateLogRequest(request);
        AIUsageLog entity = mapper.toEntity(request);
        repository.save(entity);
    }

    @Override
    public Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable) {
        aiUsageHelper.validateFilter(filter);
        return repository.findAll(aiUsageHelper.byFilter(filter), pageable)
                .map(mapper::toResponse);
    }

    @Override
    public AIUsageSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfLast7Days = LocalDate.now().minusDays(6).atStartOfDay();

        List<AIUsageLog> todayLogs = repository.findAll(
                aiUsageHelper.byFilter(aiUsageHelper.fromDateFilter(startOfToday))
        );
        List<AIUsageLog> last7DayLogs = repository.findAll(
                aiUsageHelper.byFilter(aiUsageHelper.fromDateFilter(startOfLast7Days))
        );

        return mapper.toSummaryResponse(todayLogs, last7DayLogs);
    }
}
