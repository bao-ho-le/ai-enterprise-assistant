package com.enterprise.aiassistant.backend.ai.analytics.usage.service;

import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response.AIUsageSummaryResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.entity.AIUsageLog;
import com.enterprise.aiassistant.backend.ai.analytics.usage.event.AIUsageLogEvent;
import com.enterprise.aiassistant.backend.ai.analytics.usage.helper.AIUsageHelper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.helper.AIUsageScopeHelper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.mapper.AIUsageLogMapper;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageDailyProjection;
import com.enterprise.aiassistant.backend.ai.analytics.usage.repository.AIUsageLogRepository;
import com.enterprise.aiassistant.backend.auth.security.UserPrincipal;
import com.enterprise.aiassistant.backend.auth.service.CurrentUserService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthorizationException;
import com.enterprise.aiassistant.backend.common.exception.business_exception.BusinessException;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AIUsageLogServiceImplTest {

    @Mock
    private AIUsageLogMapper aiUsageLogMapper;

    @Mock
    private AIUsageLogRepository aiUsageLogRepository;

    @Mock
    private AIUsageHelper aiUsageHelper;

    @Mock
    private AIUsageScopeHelper aiUsageScopeHelper;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private AIUsageLogServiceImpl service;

    // User id 7 thuộc phòng ban id 3
    private final UserPrincipal principal = UserPrincipal.from(
            User.builder().id(7L).department(Department.builder().id(3L).build()).build()
    );

    // ===================== logAiUsage =====================

    // TODO (bạn tự viết): logAiUsage_noUserId_attachesCurrentUser
    // Request chưa có userId (AIUsageLogRequest.builder().model("gemini").build()),
    // currentUserService.getCurrentPrincipal() trả principal ở trên
    // → event được publish phải mang request có userId = 7 và departmentId = 3.
    // Gợi ý: bắt event bằng ArgumentCaptor (xem appendTurn_noMemoryYet_createsAndSavesNewMemory
    // trong ConversationMemoryServiceImplTest):
    //   ArgumentCaptor<AIUsageLogEvent> captor = ArgumentCaptor.forClass(AIUsageLogEvent.class);
    //   verify(applicationEventPublisher).publishEvent(captor.capture());
    //   rồi kiểm tra captor.getValue().request().getUserId() ...
    @Test
    void logAiUsage_noUserId_attachesCurrentUser() {
        AIUsageLogRequest request = AIUsageLogRequest.builder().model("gemini").build();

        when(currentUserService.getCurrentPrincipal()).thenReturn(principal);

        service.logAiUsage(request);

        ArgumentCaptor<AIUsageLogEvent> captor = ArgumentCaptor.forClass(AIUsageLogEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        AIUsageLogEvent event = captor.getValue();

        assertThat(event.request().getUserId()).isEqualTo(7L);
        assertThat(event.request().getDepartmentId()).isEqualTo(3L);
    }

    @Test
    void logAiUsage_hasUserId_keepsOwnerWithoutLookup() {
        AIUsageLogRequest request = AIUsageLogRequest.builder().userId(9L).model("gemini").build();

        service.logAiUsage(request);

        assertThat(request.getUserId()).isEqualTo(9L);
        verify(currentUserService, never()).getCurrentPrincipal();
        // AIUsageLogEvent là record nên so sánh bằng equals được, không cần ArgumentCaptor
        verify(applicationEventPublisher).publishEvent(new AIUsageLogEvent(request));
    }

    // Mục đích của test này là kiểm tra logAiUsage vẫn được gọi bình thường trong trường hợp không có user
    // Vì ở bước documentProcessingExecutor (tách chữ, chunking, embedding) chạy một Thread khác với ThreadLocal (luồng chính) do Async
    // Nghĩa là SecurityContext có lưu người dùng hiện tại ở ThreadLocal lại không được Thread ở Async nhìn thấy
    @Test
    void logAiUsage_noLoggedInUser_stillPublishesEvent() {
        AIUsageLogRequest request = AIUsageLogRequest.builder().model("gemini").build();

        // Tiến trình nền không có user đăng nhập: CurrentUserService ném lỗi, service phải bỏ qua
        when(currentUserService.getCurrentPrincipal())
                .thenThrow(new AuthorizationException(ErrorCode.AUTHENTICATION_REQUIRED));

        service.logAiUsage(request);

        assertThat(request.getUserId()).isNull();
        verify(applicationEventPublisher).publishEvent(new AIUsageLogEvent(request));
    }

    @Test
    void logAiUsage_invalidRequest_doesNotPublishEvent() {
        doThrow(new BusinessException(ErrorCode.AI_USAGE_REQUEST_REQUIRED))
                .when(aiUsageHelper).validateLogRequest(null);

        assertThatThrownBy(() -> service.logAiUsage(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_REQUEST_REQUIRED);

        verifyNoInteractions(applicationEventPublisher);
    }

    // ===================== onAIUsageLogEvent =====================

    @Test
    void onAIUsageLogEvent_mapsRequestAndSaves() {
        AIUsageLogRequest request = AIUsageLogRequest.builder().model("gemini").build();
        AIUsageLog entity = AIUsageLog.builder().model("gemini").build();

        when(aiUsageLogMapper.toEntity(request)).thenReturn(entity);

        service.onAIUsageLogEvent(new AIUsageLogEvent(request));

        verify(aiUsageLogRepository).save(entity);
    }

    // ===================== getUsageLogs =====================

    @Test
    void getUsageLogs_appliesScopeBeforeQuerying() {
        AIUsageLogFilterRequest filter = new AIUsageLogFilterRequest();
        Pageable pageable = PageRequest.of(0, 10);
        AIUsageLog log = AIUsageLog.builder().model("gemini").build();
        AIUsageLogResponse response = AIUsageLogResponse.builder().model("gemini").build();

        when(currentUserService.getCurrentPrincipal()).thenReturn(principal);
        when(aiUsageLogRepository.filterUsageLogs(filter, pageable)).thenReturn(new PageImpl<>(List.of(log)));
        when(aiUsageLogMapper.toResponse(log)).thenReturn(response);

        Page<AIUsageLogResponse> result = service.getUsageLogs(filter, pageable);

        assertThat(result.getContent()).containsExactly(response);

        // Giới hạn phạm vi dữ liệu (chỉ xem log của mình/phòng ban) phải chạy TRƯỚC khi query
        InOrder inOrder = inOrder(aiUsageScopeHelper, aiUsageLogRepository);
        inOrder.verify(aiUsageScopeHelper).applyScope(filter, principal);
        inOrder.verify(aiUsageLogRepository).filterUsageLogs(filter, pageable);
    }

    // ===================== getSummary =====================

    @Test
    void getSummary_queriesTodayAndLast7Days() {
        AIUsageLogFilterRequest filter = new AIUsageLogFilterRequest();
        List<AIUsageLog> todayLogs = List.of(AIUsageLog.builder().model("a").build());
        List<AIUsageLog> last7DayLogs = List.of(AIUsageLog.builder().model("b").build());
        AIUsageSummaryResponse summary = AIUsageSummaryResponse.builder().todayRequest(1).build();

        when(aiUsageHelper.fromDateFilter(any())).thenReturn(filter);
        when(currentUserService.getCurrentPrincipal()).thenReturn(principal);
        // Gọi lần 1 trả todayLogs, lần 2 trả last7DayLogs
        when(aiUsageLogRepository.filterUsageLogs(filter)).thenReturn(todayLogs, last7DayLogs);
        when(aiUsageLogMapper.toSummaryResponse(todayLogs, last7DayLogs)).thenReturn(summary);

        assertThat(service.getSummary()).isSameAs(summary);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(aiUsageHelper, times(2)).fromDateFilter(fromCaptor.capture());
        assertThat(fromCaptor.getAllValues()).containsExactly(
                LocalDate.now().atStartOfDay(),
                LocalDate.now().minusDays(6).atStartOfDay()
        );
    }

    // ===================== getDailyUsage =====================

    @Test
    void getDailyUsage_dayWithoutData_passesNullRow() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(2).atStartOfDay();

        // Projection là interface, tạo object giả bằng mock(...) thay vì @Mock
        AIUsageDailyProjection todayRow = mock(AIUsageDailyProjection.class);
        when(todayRow.getDay()).thenReturn(today);

        when(aiUsageHelper.fromDateFilter(start)).thenReturn(new AIUsageLogFilterRequest());
        when(currentUserService.getCurrentPrincipal()).thenReturn(principal);
        when(aiUsageLogRepository.findDailyStats(start, null, null)).thenReturn(List.of(todayRow));

        assertThat(service.getDailyUsage(3)).hasSize(3);

        // 2 ngày không có dữ liệu vẫn có mặt trong kết quả, chỉ là row = null
        // null ở đây là kết quả trả về từ Map khi cố getKey không có, nên map trả về null, ở đây assert null cho giống kết quả từ Map trả về
        verify(aiUsageLogMapper).toDailyResponse(today.minusDays(2), null);
        verify(aiUsageLogMapper).toDailyResponse(today.minusDays(1), null);
        verify(aiUsageLogMapper).toDailyResponse(today, todayRow);
    }

    @Test
    void getDailyUsage_invalidDays_doesNotQuery() {
        doThrow(new BusinessException(ErrorCode.AI_USAGE_INVALID_DAYS)).when(aiUsageHelper).validateDays(0);

        assertThatThrownBy(() -> service.getDailyUsage(0))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.AI_USAGE_INVALID_DAYS);

        verifyNoInteractions(aiUsageLogRepository);
    }

    // ===================== getDistinctModels =====================

    @Test
    void getDistinctModels_hasPermission_returnsModels() {
        when(aiUsageLogRepository.findDistinctModels()).thenReturn(List.of("gemini-2.5-flash", "gemini-embedding"));

        assertThat(service.getDistinctModels()).containsExactly("gemini-2.5-flash", "gemini-embedding");
    }

    @Test
    void getDistinctModels_noPermission_throwsPermissionDenied() {
        doThrow(new AuthorizationException(ErrorCode.PERMISSION_DENIED))
                .when(currentUserService).requirePermission(Permission.AI_USAGE_READ_SELF);

        assertThatThrownBy(() -> service.getDistinctModels())
                .isInstanceOf(AuthorizationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PERMISSION_DENIED);

        verifyNoInteractions(aiUsageLogRepository);
    }
}
