package com.enterprise.aiassistant.backend.admin.usage.controller;

import com.enterprise.aiassistant.backend.admin.usage.dto.AIUsageGroupResponse;
import com.enterprise.aiassistant.backend.admin.usage.dto.AIUsageOverviewResponse;
import com.enterprise.aiassistant.backend.admin.usage.service.AdminUsageService;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.request.AIUsageLogFilterRequest;
import com.enterprise.aiassistant.backend.ai.analytics.usage.dto.response.AIUsageLogResponse;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.ConversationType;
import com.enterprise.aiassistant.backend.ai.analytics.usage.service.AIUsageLogService;
import com.enterprise.aiassistant.backend.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminUsageController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.enterprise\\.aiassistant\\.backend\\.auth\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "api.prefix=/api/v1")
class AdminUsageControllerTest {

    private static final String BASE = "/api/v1/admin/ai-usage";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUsageService adminUsageService;

    @MockitoBean
    private AIUsageLogService aiUsageLogService;

    // ---------- helper ----------

    private AIUsageOverviewResponse fakeOverview() {
        return AIUsageOverviewResponse.builder()
                .totalRequests(120L)
                .totalTokens(45_000L)
                .totalCost(new BigDecimal("12.5"))
                .byUser(List.of(AIUsageGroupResponse.builder()
                        .id(7L).name("huy").requestCount(80L)
                        .totalTokens(30_000L).cost(new BigDecimal("8.5")).build()))
                .byDepartment(List.of(AIUsageGroupResponse.builder()
                        .id(10L).name("Engineering").requestCount(120L)
                        .totalTokens(45_000L).cost(new BigDecimal("12.5")).build()))
                .build();
    }

    private AIUsageLogResponse fakeLog() {
        return AIUsageLogResponse.builder()
                .userId(7L)
                .departmentId(10L)
                .createdAt(LocalDateTime.of(2026, 1, 15, 9, 30))
                .conversationType(ConversationType.values()[0])
                .model("gemini-2.5-flash")
                .inputTokens(100)
                .outputTokens(50)
                .totalTokens(150)
                .estimatedCost(new BigDecimal("0.25"))
                .status(AIUsageStatus.values()[0])
                .build();
    }

    private void stubEmptyLogs() {
        when(aiUsageLogService.getUsageLogs(any(AIUsageLogFilterRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<AIUsageLogResponse>(List.of()));
    }

    // ================= OVERVIEW =================

    @Test
    void overview_khongThamSo_tatCaLaNull_vaTraDungJson() throws Exception {
        when(adminUsageService.getOverview(any(), any(), any(), any())).thenReturn(fakeOverview());

        mockMvc.perform(get(BASE + "/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests").value(120))
                .andExpect(jsonPath("$.totalTokens").value(45000))
                .andExpect(jsonPath("$.totalCost").value(12.5))
                .andExpect(jsonPath("$.byUser.length()").value(1))
                .andExpect(jsonPath("$.byUser[0].id").value(7))
                .andExpect(jsonPath("$.byUser[0].name").value("huy"))
                .andExpect(jsonPath("$.byUser[0].requestCount").value(80))
                .andExpect(jsonPath("$.byUser[0].totalTokens").value(30000))
                .andExpect(jsonPath("$.byUser[0].cost").value(8.5))
                .andExpect(jsonPath("$.byDepartment[0].name").value("Engineering"));

        verify(adminUsageService).getOverview(isNull(), isNull(), isNull(), isNull());
        verifyNoInteractions(aiUsageLogService);
    }

    @Test
    void overview_truyenDayDuThamSo_duocBindDungKieu() throws Exception {
        when(adminUsageService.getOverview(any(), any(), any(), any())).thenReturn(fakeOverview());

        mockMvc.perform(get(BASE + "/overview")
                        .param("fromDate", "2026-01-01T00:00:00")
                        .param("toDate", "2026-01-31T23:59:59")
                        .param("userId", "7")
                        .param("departmentId", "10"))
                .andExpect(status().isOk());

        verify(adminUsageService).getOverview(
                eq(LocalDateTime.of(2026, 1, 1, 0, 0, 0)),
                eq(LocalDateTime.of(2026, 1, 31, 23, 59, 59)),
                eq(7L),
                eq(10L));
    }

    @Test
    void overview_fromDateSaiDinhDang_tra400_TYPE_MISMATCH() throws Exception {
        mockMvc.perform(get(BASE + "/overview").param("fromDate", "01/01/2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value(containsString("fromDate")));

        verifyNoInteractions(adminUsageService);
    }

    @Test
    void overview_chiCoNgayKhongCoGio_tra400_vi_yeuCauISO_DATE_TIME() throws Exception {
        mockMvc.perform(get(BASE + "/overview").param("toDate", "2026-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"));

        verifyNoInteractions(adminUsageService);
    }

    @Test
    void overview_userIdKhongPhaiSo_tra400() throws Exception {
        mockMvc.perform(get(BASE + "/overview").param("userId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value(containsString("userId")));

        verifyNoInteractions(adminUsageService);
    }

    // ================= LOGS =================

    @Test
    void getUsageLogs_coDuLieu_traDungTungFieldCuaLog() throws Exception {
        when(aiUsageLogService.getUsageLogs(any(AIUsageLogFilterRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fakeLog())));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(7))
                .andExpect(jsonPath("$.content[0].departmentId").value(10))
                .andExpect(jsonPath("$.content[0].model").value("gemini-2.5-flash"))
                .andExpect(jsonPath("$.content[0].inputTokens").value(100))
                .andExpect(jsonPath("$.content[0].outputTokens").value(50))
                .andExpect(jsonPath("$.content[0].totalTokens").value(150))
                .andExpect(jsonPath("$.content[0].estimatedCost").value(0.25))
                .andExpect(jsonPath("$.content[0].conversationType")
                        .value(ConversationType.values()[0].name()))
                .andExpect(jsonPath("$.content[0].status")
                        .value(AIUsageStatus.values()[0].name()))
                .andExpect(jsonPath("$.content[0].createdAt").exists());
    }

    @Test
    void getUsageLogs_khongThamSo_filterRongVaMacDinhPage0Size20() throws Exception {
        stubEmptyLogs();

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        ArgumentCaptor<AIUsageLogFilterRequest> filterCaptor =
                ArgumentCaptor.forClass(AIUsageLogFilterRequest.class);
        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(aiUsageLogService).getUsageLogs(filterCaptor.capture(), pageCaptor.capture());

        AIUsageLogFilterRequest f = filterCaptor.getValue();
        assertNotNull(f);
        assertNull(f.getFromDate());
        assertNull(f.getToDate());
        assertNull(f.getModel());
        assertNull(f.getUserId());
        assertNull(f.getDepartmentId());
        assertNull(f.getConversationType());
        assertNull(f.getStatus());
        assertEquals(0, pageCaptor.getValue().getPageNumber());
        assertEquals(20, pageCaptor.getValue().getPageSize());
        verifyNoInteractions(adminUsageService);
    }

    @Test
    void getUsageLogs_truyenPageVaSize_duocChuyenVaoService() throws Exception {
        stubEmptyLogs();

        mockMvc.perform(get(BASE).param("page", "3").param("size", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(aiUsageLogService).getUsageLogs(any(AIUsageLogFilterRequest.class), pageCaptor.capture());
        assertEquals(3, pageCaptor.getValue().getPageNumber());
        assertEquals(10, pageCaptor.getValue().getPageSize());
    }

    @Test
    void getUsageLogs_truyenDayDuFilter_duocBindDungTungField() throws Exception {
        stubEmptyLogs();

        mockMvc.perform(get(BASE)
                        .param("fromDate", "2026-01-01T00:00:00")
                        .param("toDate", "2026-01-31T23:59:59")
                        .param("conversationType", ConversationType.values()[0].name())
                        .param("model", "gemini-2.5-pro")
                        .param("status", AIUsageStatus.values()[0].name())
                        .param("userId", "7")
                        .param("departmentId", "10"))
                .andExpect(status().isOk());

        ArgumentCaptor<AIUsageLogFilterRequest> captor =
                ArgumentCaptor.forClass(AIUsageLogFilterRequest.class);
        verify(aiUsageLogService).getUsageLogs(captor.capture(), any(Pageable.class));

        AIUsageLogFilterRequest f = captor.getValue();
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0, 0), f.getFromDate());
        assertEquals(LocalDateTime.of(2026, 1, 31, 23, 59, 59), f.getToDate());
        assertEquals(ConversationType.values()[0], f.getConversationType());
        assertEquals("gemini-2.5-pro", f.getModel());
        assertEquals(AIUsageStatus.values()[0], f.getStatus());
        assertEquals(7L, f.getUserId());
        assertEquals(10L, f.getDepartmentId());
    }

    @Test
    void getUsageLogs_fromDateSauToDate_tra400_vaKhongGoiService() throws Exception {
        mockMvc.perform(get(BASE)
                        .param("fromDate", "2026-02-01T00:00:00")
                        .param("toDate", "2026-01-01T00:00:00"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("fromDate phải nhỏ hơn hoặc bằng toDate")));

        verifyNoInteractions(aiUsageLogService);
    }

    @Test
    void getUsageLogs_fromDateBangToDate_hopLe() throws Exception {
        stubEmptyLogs();

        mockMvc.perform(get(BASE)
                        .param("fromDate", "2026-01-01T10:00:00")
                        .param("toDate", "2026-01-01T10:00:00"))
                .andExpect(status().isOk());

        verify(aiUsageLogService).getUsageLogs(any(AIUsageLogFilterRequest.class), any(Pageable.class));
    }

    @Test
    void getUsageLogs_chiCoMotTrongHaiNgay_hopLe() throws Exception {
        stubEmptyLogs();

        mockMvc.perform(get(BASE).param("fromDate", "2026-01-01T00:00:00"))
                .andExpect(status().isOk());

        verify(aiUsageLogService).getUsageLogs(any(AIUsageLogFilterRequest.class), any(Pageable.class));
    }

    @Test
    void getUsageLogs_modelDaiHon100KyTu_tra400() throws Exception {
        mockMvc.perform(get(BASE).param("model", "m".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("model")))
                .andExpect(jsonPath("$.message").value(containsString("Tên model không được vượt quá 100 ký tự")));

        verifyNoInteractions(aiUsageLogService);
    }

    @Test
    void getUsageLogs_conversationTypeKhongHopLe_tra400() throws Exception {
        mockMvc.perform(get(BASE).param("conversationType", "KHONG_TON_TAI"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiUsageLogService);
    }

    @Test
    void getUsageLogs_userIdKhongPhaiSo_tra400() throws Exception {
        mockMvc.perform(get(BASE).param("userId", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(aiUsageLogService);
    }

    // ================= MODELS =================

    @Test
    void getModels_tra200_vaDanhSachTen() throws Exception {
        when(aiUsageLogService.getDistinctModels()).thenReturn(List.of("gemini-2.5-flash", "gemini-2.5-pro"));

        mockMvc.perform(get(BASE + "/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("gemini-2.5-flash"))
                .andExpect(jsonPath("$[1]").value("gemini-2.5-pro"));

        verify(aiUsageLogService).getDistinctModels();
        verify(aiUsageLogService, never()).getUsageLogs(any(), any());
    }

    @Test
    void getModels_danhSachRong_tra200_mangRong() throws Exception {
        when(aiUsageLogService.getDistinctModels()).thenReturn(List.of());

        mockMvc.perform(get(BASE + "/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}