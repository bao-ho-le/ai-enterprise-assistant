package com.enterprise.aiassistant.backend.ai.analytics.usage.controller;

import com.enterprise.aiassistant.backend.ai.analytics.usage.enums.AIUsageStatus;
import com.enterprise.aiassistant.backend.department.entity.Department;
import com.enterprise.aiassistant.backend.support.IntegrationTestBase;
import com.enterprise.aiassistant.backend.user.entity.User;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration test: request đi qua đủ các tầng thật Security → Controller → Service → Repository → Postgres.
// Chỉ có Gemini và Qdrant là giả (xem IntegrationTestBase).
class AIUsageLogControllerIT extends IntegrationTestBase {

    private static final String URL = "/api/v1/ai-usage";

    // ===================== GET /ai-usage =====================

    // Bảo mật: chỉ có quyền READ_SELF thì chỉ thấy log của chính mình
    @Test
    void getUsageLogs_selfScope_returnsOnlyOwnLogs() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        data.createUsageLog(alice, "gemini-a", AIUsageStatus.SUCCESS, 10, 5);
        data.createUsageLog(alice, "gemini-a", AIUsageStatus.SUCCESS, 20, 5);
        data.createUsageLog(bob, "gemini-a", AIUsageStatus.SUCCESS, 30, 5);

        mockMvc.perform(get(URL).with(loginAs(alice, Permission.AI_USAGE_READ_SELF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].userId", everyItem(is(alice.getId().intValue()))));
    }

    // Bảo mật: tự sửa query string ?userId=... để xem log người khác cũng không được,
    // vì AIUsageScopeHelper ghi đè userId theo người đang đăng nhập
    @Test
    void getUsageLogs_selfScopeWithOtherUserId_ignoresRequestedUser() throws Exception {
        User alice = data.createUser("alice");
        User bob = data.createUser("bob");
        data.createUsageLog(alice, "gemini-a", AIUsageStatus.SUCCESS, 10, 5);
        data.createUsageLog(bob, "gemini-a", AIUsageStatus.SUCCESS, 30, 5);

        mockMvc.perform(get(URL)
                        .param("userId", bob.getId().toString())
                        .with(loginAs(alice, Permission.AI_USAGE_READ_SELF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(alice.getId()));
    }

    // Bị chặn ngay ở @Valid (DTO có @AssertTrue validDateRange) trước khi vào service,
    // nên mã lỗi là VALIDATION_FAILED chứ không phải INVALID_DATE_RANGE của AIUsageHelper
    @Test
    void getUsageLogs_fromDateAfterToDate_returns400() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(get(URL)
                        .param("fromDate", "2026-11-10T00:00:00")
                        .param("toDate", "2026-11-01T00:00:00")
                        .with(loginAs(alice, Permission.AI_USAGE_READ_SELF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"));
    }

    // Đã đăng nhập nhưng không có permission AI_USAGE_* nào → 403 PERMISSION_DENIED
    @Test
    void getUsageLogs_noUsagePermission_returns403() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(get(URL).with(loginAs(alice, Permission.AI_CHAT)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("PERMISSION_DENIED"));
    }

    // Không gắn loginAs(...): Spring Security chặn trước khi request tới controller
    @Test
    void getUsageLogs_notLoggedIn_returns401() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    // TODO (bạn tự viết): getUsageLogs_departmentScope_returnsDepartmentLogs
    // Quyền AI_USAGE_READ_DEPARTMENT thì thấy log của cả phòng ban mình, nhưng không thấy phòng ban khác.
    // Chuẩn bị:
    //   Department sales = data.createDepartment("sales"); Department hr = data.createDepartment("hr");
    //   alice và bob thuộc sales (data.createUser("alice", sales)), carol thuộc hr.
    //   Mỗi người 1 log (data.createUsageLog(...)).
    // Gọi GET với loginAs(alice, Permission.AI_USAGE_READ_DEPARTMENT)
    // → 200, $.content có 2 phần tử, mọi $.content[*].departmentId bằng sales.getId().
    // Gợi ý: làm giống getUsageLogs_selfScope_returnsOnlyOwnLogs, đổi userId thành departmentId.
    @Test
    void getUsageLogs_departmentScope_returnsDepartmentLogs() throws Exception {
        Department sales = data.createDepartment("sales");
        Department hr = data.createDepartment("hr");
        User alice = data.createUser("alice", sales);
        User bob = data.createUser("bob", sales);
        User carol = data.createUser("carol", hr);

        data.createUsageLog(alice, "gemini-a", AIUsageStatus.SUCCESS, 10, 5);
        data.createUsageLog(bob, "gemini-a", AIUsageStatus.SUCCESS, 10, 5);
        data.createUsageLog(carol, "gemini-a", AIUsageStatus.SUCCESS, 10, 5);

        mockMvc.perform(get(URL).with(loginAs(alice, Permission.AI_USAGE_READ_DEPARTMENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[*].departmentId", everyItem(is(sales.getId().intValue()))));
    }

    // ===================== GET /ai-usage/summary =====================

    @Test
    void getSummary_todayLogs_countsRequestsTokensAndSuccessRate() throws Exception {
        User alice = data.createUser("alice");
        data.createUsageLog(alice, "gemini-a", AIUsageStatus.SUCCESS, 10, 5);
        data.createUsageLog(alice, "gemini-a", AIUsageStatus.FAILED, 0, 0);

        mockMvc.perform(get(URL + "/summary").with(loginAs(alice, Permission.AI_USAGE_READ_SELF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayRequest").value(2))
                .andExpect(jsonPath("$.todayToken").value(15))
                .andExpect(jsonPath("$.todaySuccessRate").value(50.0))
                .andExpect(jsonPath("$.last7DayRequests").value(2));
    }

    // ===================== GET /ai-usage/daily =====================

    // Ngày không có log vẫn phải có một dòng (requestCount = 0) để biểu đồ không bị hụt ngày
    @Test
    void getDailyUsage_threeDays_returnsOneRowPerDayEndingToday() throws Exception {
        User alice = data.createUser("alice");
        data.createUsageLog(alice, "gemini-a", AIUsageStatus.SUCCESS, 10, 5);

        mockMvc.perform(get(URL + "/daily")
                        .param("days", "3")
                        .with(loginAs(alice, Permission.AI_USAGE_READ_SELF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].requestCount").value(0))
                .andExpect(jsonPath("$[2].date").value(LocalDate.now().toString()))
                .andExpect(jsonPath("$[2].requestCount").value(1))
                .andExpect(jsonPath("$[2].totalTokens").value(15));
    }

    @Test
    void getDailyUsage_daysOutOfRange_returns400() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(get(URL + "/daily")
                        .param("days", "91")
                        .with(loginAs(alice, Permission.AI_USAGE_READ_SELF)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("AI_USAGE_INVALID_DAYS"));
    }

    // ===================== GET /ai-usage/models =====================

    @Test
    void getModels_duplicateModels_returnsEachModelOnceSorted() throws Exception {
        User alice = data.createUser("alice");
        data.createUsageLog(alice, "gemini-b", AIUsageStatus.SUCCESS, 1, 1);
        data.createUsageLog(alice, "gemini-a", AIUsageStatus.SUCCESS, 1, 1);
        data.createUsageLog(alice, "gemini-b", AIUsageStatus.SUCCESS, 1, 1);

        mockMvc.perform(get(URL + "/models").with(loginAs(alice, Permission.AI_USAGE_READ_SELF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", contains("gemini-a", "gemini-b")));
    }

    @Test
    void getModels_noUsagePermission_returns403() throws Exception {
        User alice = data.createUser("alice");

        mockMvc.perform(get(URL + "/models").with(loginAs(alice)))
                .andExpect(status().isForbidden());
    }
}
