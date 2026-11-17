package com.enterprise.aiassistant.backend.admin.role.controller;

import com.enterprise.aiassistant.backend.admin.role.dto.RolePermissionResponse;
import com.enterprise.aiassistant.backend.admin.role.service.AdminRoleService;
import com.enterprise.aiassistant.backend.common.exception.GlobalExceptionHandler;
import com.enterprise.aiassistant.backend.user.enums.Permission;
import com.enterprise.aiassistant.backend.user.enums.Role;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminRoleController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.enterprise\\.aiassistant\\.backend\\.auth\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "api.prefix=/api/v1")
class AdminRoleControllerTest {

    private static final String BASE = "/api/v1/admin/roles";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminRoleService adminRoleService;

    private RolePermissionResponse fakeRoleResponse(String role, String... permissions) {
        return RolePermissionResponse.builder()
                .role(role)
                .permissions(List.of(permissions))
                .build();
    }

    // ================= GET /roles =================

    @Test
    void getRoles_tra200_vaDungNoiDungTungRole() throws Exception {
        when(adminRoleService.getRoles()).thenReturn(List.of(
                fakeRoleResponse("ADMIN", "USER_READ", "USER_CREATE"),
                fakeRoleResponse("MANAGER", "DOCUMENT_READ")));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].permissions.length()").value(2))
                .andExpect(jsonPath("$[0].permissions[0]").value("USER_READ"))
                .andExpect(jsonPath("$[1].role").value("MANAGER"))
                .andExpect(jsonPath("$[1].permissions[0]").value("DOCUMENT_READ"));

        verify(adminRoleService).getRoles();
    }

    @Test
    void getRoles_danhSachRong_tra200_mangRong() throws Exception {
        when(adminRoleService.getRoles()).thenReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ================= GET /roles/permissions =================

    @Test
    void getPermissionCatalog_tra200_vaKhongGoiGetRoles() throws Exception {
        when(adminRoleService.getPermissionCatalog()).thenReturn(List.of("USER_READ", "AI_CHAT"));

        mockMvc.perform(get(BASE + "/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("USER_READ"))
                .andExpect(jsonPath("$[1]").value("AI_CHAT"));

        verify(adminRoleService).getPermissionCatalog();
        verify(adminRoleService, never()).getRoles();
    }

    // ================= PUT /roles/{role}/permissions =================

    @Test
    void updateRolePermissions_truyenDungRole_vaDanhSachPermission() throws Exception {
        when(adminRoleService.updateRolePermissions(eq(Role.MANAGER), any()))
                .thenReturn(fakeRoleResponse("MANAGER", "DOCUMENT_READ", "AI_CHAT"));

        mockMvc.perform(put(BASE + "/MANAGER/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[\"DOCUMENT_READ\",\"AI_CHAT\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.permissions.length()").value(2))
                .andExpect(jsonPath("$.permissions[1]").value("AI_CHAT"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Permission>> captor = ArgumentCaptor.forClass(List.class);
        verify(adminRoleService).updateRolePermissions(eq(Role.MANAGER), captor.capture());
        assertEquals(List.of(Permission.DOCUMENT_READ, Permission.AI_CHAT), captor.getValue());
    }

    @Test
    void updateRolePermissions_danhSachRong_chuyenListRongChoService() throws Exception {
        when(adminRoleService.updateRolePermissions(eq(Role.EMPLOYEE), any()))
                .thenReturn(fakeRoleResponse("EMPLOYEE"));

        mockMvc.perform(put(BASE + "/EMPLOYEE/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissions.length()").value(0));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Permission>> captor = ArgumentCaptor.forClass(List.class);
        verify(adminRoleService).updateRolePermissions(eq(Role.EMPLOYEE), captor.capture());
        assertEquals(0, captor.getValue().size());
    }

    @Test
    void updateRolePermissions_bodyRong_permissionsLaNull_serviceNhanNull() throws Exception {
        // Ghi nhận hành vi hiện tại: không có validation nên null lọt xuống service.
        when(adminRoleService.updateRolePermissions(eq(Role.ADMIN), any()))
                .thenReturn(fakeRoleResponse("ADMIN"));

        mockMvc.perform(put(BASE + "/ADMIN/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Permission>> captor = ArgumentCaptor.forClass(List.class);
        verify(adminRoleService).updateRolePermissions(eq(Role.ADMIN), captor.capture());
        assertNull(captor.getValue());
    }

    @Test
    void updateRolePermissions_khongCoBody_tra400_nhanhNullCuaControllerLaCodeChet() throws Exception {
        mockMvc.perform(put(BASE + "/MANAGER/permissions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));

        verifyNoInteractions(adminRoleService);
    }

    @Test
    void updateRolePermissions_roleTrenUrlKhongHopLe_tra400_TYPE_MISMATCH() throws Exception {
        mockMvc.perform(put(BASE + "/KHONG_TON_TAI/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value(containsString("role")));

        verifyNoInteractions(adminRoleService);
    }

    @Test
    void updateRolePermissions_permissionKhongHopLe_tra400_vaKhongGoiService() throws Exception {
        mockMvc.perform(put(BASE + "/MANAGER/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permissions\":[\"KHONG_TON_TAI\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));

        verifyNoInteractions(adminRoleService);
    }
}