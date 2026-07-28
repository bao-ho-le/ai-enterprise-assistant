package com.enterprise.aiassistant.backend.ai.usage.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.enterprise.aiassistant.backend.ai.conversation.entity.AIConversation;
import com.enterprise.aiassistant.backend.ai.conversation.entity.AIMessage;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIConversationRepository;
import com.enterprise.aiassistant.backend.ai.conversation.repository.AIMessageRepository;

import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageDailyResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.usage.helper.AiUsageHelper;
import com.enterprise.aiassistant.backend.ai.usage.mapper.AIUsageLogMapper;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageDailyProjection;
import com.enterprise.aiassistant.backend.ai.usage.repository.AIUsageLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AIUsageLogServiceImpl implements AIUsageLogService {

    private final AIUsageLogMapper aiUsageLogMapper;
    private final AIUsageLogRepository aiUsageLogRepository;
    private final AiUsageHelper aiUsageHelper;

    private final AIConversationRepository aiConversationRepository;
    private final AIMessageRepository aiMessageRepository;



    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAiUsage(AIUsageLogRequest request) {
        aiUsageHelper.validateLogRequest(request);

        // Dùng cho embedding, vẫn tính log nhưng không có conversation
        AIConversation aiConversation = request.getConversationId() != null
                ? aiConversationRepository.getReferenceById(request.getConversationId())
                : null;
        AIMessage aiMessage = request.getMessageId() != null
                ? aiMessageRepository.getReferenceById(request.getMessageId())
                : null;

        AIUsageLog entity = aiUsageLogMapper.toEntity(request, aiConversation, aiMessage);

        aiUsageLogRepository.save(entity);
    }

    @Override
    public Page<AIUsageLogResponse> getUsageLogs(AIUsageLogFilterRequest filter, Pageable pageable) {
        aiUsageHelper.validateFilter(filter);

        return aiUsageLogRepository.filterUsageLogs(filter, pageable)

                .map(aiUsageLogMapper::toResponse);
    }

    @Override
    public AIUsageSummaryResponse getSummary() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfLast7Days = LocalDate.now().minusDays(6).atStartOfDay();


        List<AIUsageLog> todayLogs = aiUsageLogRepository.filterUsageLogs(aiUsageHelper.fromDateFilter(startOfToday));
        List<AIUsageLog> last7DayLogs = aiUsageLogRepository.filterUsageLogs(aiUsageHelper.fromDateFilter(startOfLast7Days));


        return aiUsageLogMapper.toSummaryResponse(todayLogs, last7DayLogs);
    }

    @Override
    public List<AIUsageDailyResponse> getDailyUsage(int days) {
        aiUsageHelper.validateDays(days);

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(days - 1L);

        Map<LocalDate, AIUsageDailyProjection> byDay = aiUsageLogRepository
                .findDailyStats(start.atStartOfDay())
                .stream()
                .collect(Collectors.toMap(AIUsageDailyProjection::getDay, Function.identity()));

        List<AIUsageDailyResponse> result = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(aiUsageLogMapper.toDailyResponse(date, byDay.get(date)));
        }
        return result;
    }

    @Override
    public List<String> getDistinctModels() {
        return aiUsageLogRepository.findDistinctModels();
    }
}
