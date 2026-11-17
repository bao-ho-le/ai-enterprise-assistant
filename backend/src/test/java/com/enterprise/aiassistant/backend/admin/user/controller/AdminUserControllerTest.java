package com.enterprise.aiassistant.backend.admin.user.controller;

import com.enterprise.aiassistant.backend.admin.user.dto.request.AdminCreateUserRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.AdminUpdateUserRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.AssignRoleRequest;
import com.enterprise.aiassistant.backend.admin.user.dto.request.ChangeDepartmentRequest;
import com.enterprise.aiassistant.backend.admin.user.service.AdminUserService;
import com.enterprise.aiassistant.backend.common.exception.GlobalExceptionHandler;
import com.enterprise.aiassistant.backend.user.dto.response.UserResponse;
import com.enterprise.aiassistant.backend.user.enums.Role;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AdminUserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.enterprise\\.aiassistant\\.backend\\.auth\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "api.prefix=/api/v1")
class AdminUserControllerTest {

    private static final String BASE = "/api/v1/admin/users";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    // ---------- helper ----------

    private UserResponse fakeUser(Long id, String username, boolean enabled) {
        return UserResponse.builder()
                .id(id)
                .username(username)
                .email(username + "@mail.com")
                .fullName("Full " + username)
                .role("EMPLOYEE")
                .permissions(Set.of("CHAT_USE"))
                .departmentId(10L)
                .departmentName("Engineering")
                .enabled(enabled)
                .createdAt(LocalDateTime.of(2026, 1, 15, 9, 30))
                .build();
    }

    // ================= LIST =================

    @Test
    void getUsers_khongFilter_tatCaThamSoLaNull_vaMacDinhPage0Size20() throws Exception {
        when(adminUserService.getUsers(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fakeUser(1L, "an", true))));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("an"));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminUserService).getUsers(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertEquals(0, captor.getValue().getPageNumber());
        assertEquals(20, captor.getValue().getPageSize());
    }

    @Test
    void getUsers_truyenDayDuFilter_duocBindDungKieu() throws Exception {
        when(adminUserService.getUsers(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get(BASE)
                        .param("keyword", "huy")
                        .param("role", "MANAGER")
                        .param("departmentId", "10")
                        .param("enabled", "true")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(adminUserService).getUsers(
                eq("huy"), eq(Role.MANAGER), eq(10L), eq(true), captor.capture());
        assertEquals(2, captor.getValue().getPageNumber());
        assertEquals(5, captor.getValue().getPageSize());
    }

    @Test
    void getUsers_roleKhongHopLe_tra400_TYPE_MISMATCH() throws Exception {
        mockMvc.perform(get(BASE).param("role", "KHONG_TON_TAI"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value(containsString("role")));

        verifyNoInteractions(adminUserService);
    }

    @Test
    void getUsers_departmentIdKhongPhaiSo_tra400() throws Exception {
        mockMvc.perform(get(BASE).param("departmentId", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"));

        verifyNoInteractions(adminUserService);
    }

    // ================= DETAIL =================

    @Test
    void getUserDetail_tra200() throws Exception {
        when(adminUserService.getUserDetail(3L)).thenReturn(fakeUser(3L, "binh", true));

        mockMvc.perform(get(BASE + "/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("binh"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getUserDetail_idKhongPhaiSo_tra400() throws Exception {
        mockMvc.perform(get(BASE + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("TYPE_MISMATCH"))
                .andExpect(jsonPath("$.message").value(containsString("userId")));

        verifyNoInteractions(adminUserService);
    }

    // ================= CREATE =================

    @Test
    void createUser_tra200_vaBodyDuocBindDungTungField() throws Exception {
        when(adminUserService.createUser(any(AdminCreateUserRequest.class)))
                .thenReturn(fakeUser(100L, "moi", true));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"moi","email":"moi@mail.com","fullName":"Moi",
                                 "password":"12345678","role":"SUPERVISOR","departmentId":20}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.username").value("moi"));

        ArgumentCaptor<AdminCreateUserRequest> captor = ArgumentCaptor.forClass(AdminCreateUserRequest.class);
        verify(adminUserService).createUser(captor.capture());
        AdminCreateUserRequest sent = captor.getValue();
        assertEquals("moi", sent.getUsername());
        assertEquals("moi@mail.com", sent.getEmail());
        assertEquals("Moi", sent.getFullName());
        assertEquals("12345678", sent.getPassword());
        assertEquals(Role.SUPERVISOR, sent.getRole());
        assertEquals(20L, sent.getDepartmentId());
    }

    @Test
    void createUser_khongCoBody_tra400_MALFORMED_REQUEST_BODY() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));

        verifyNoInteractions(adminUserService);
    }

    @Test
    void createUser_roleKhongHopLe_tra400() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"moi\",\"role\":\"USER\"}"))   // USER không có trong enum Role
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));

        verifyNoInteractions(adminUserService);
    }

    // ================= UPDATE =================

    @Test
    void updateUser_truyenDungUserId_vaBodyDuocBindDungField() throws Exception {
        when(adminUserService.updateUser(eq(5L), any(AdminUpdateUserRequest.class)))
                .thenReturn(fakeUser(5L, "an", true));

        mockMvc.perform(put(BASE + "/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ten Moi\",\"email\":\"moi@mail.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<AdminUpdateUserRequest> captor = ArgumentCaptor.forClass(AdminUpdateUserRequest.class);
        verify(adminUserService).updateUser(eq(5L), captor.capture());
        assertEquals("Ten Moi", captor.getValue().getFullName());
        assertEquals("moi@mail.com", captor.getValue().getEmail());
    }

    // ================= ENABLE / DISABLE =================

    @Test
    void enableUser_goiSetEnabledVoiTrue() throws Exception {
        when(adminUserService.setEnabled(7L, true)).thenReturn(fakeUser(7L, "an", true));

        mockMvc.perform(put(BASE + "/7/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(adminUserService).setEnabled(7L, true);
        verify(adminUserService, never()).setEnabled(anyLong(), eq(false));
    }

    @Test
    void disableUser_goiSetEnabledVoiFalse() throws Exception {
        when(adminUserService.setEnabled(7L, false)).thenReturn(fakeUser(7L, "an", false));

        mockMvc.perform(put(BASE + "/7/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));

        verify(adminUserService).setEnabled(7L, false);
        verify(adminUserService, never()).setEnabled(anyLong(), eq(true));
    }

    // ================= ROLE =================

    @Test
    void assignRole_truyenDungUserId_vaRole() throws Exception {
        when(adminUserService.assignRole(eq(5L), any(AssignRoleRequest.class)))
                .thenReturn(fakeUser(5L, "an", true));

        mockMvc.perform(put(BASE + "/5/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<AssignRoleRequest> captor = ArgumentCaptor.forClass(AssignRoleRequest.class);
        verify(adminUserService).assignRole(eq(5L), captor.capture());
        assertEquals(Role.ADMIN, captor.getValue().getRole());
    }

    @Test
    void assignRole_roleKhongHopLe_tra400_vaKhongGoiService() throws Exception {
        mockMvc.perform(put(BASE + "/5/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"KHONG_TON_TAI\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MALFORMED_REQUEST_BODY"));

        verifyNoInteractions(adminUserService);
    }

    // ================= DEPARTMENT =================

    @Test
    void changeDepartment_truyenDungUserId_vaDepartmentId() throws Exception {
        when(adminUserService.changeDepartment(eq(5L), any(ChangeDepartmentRequest.class)))
                .thenReturn(fakeUser(5L, "an", true));

        mockMvc.perform(put(BASE + "/5/department")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":20}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        ArgumentCaptor<ChangeDepartmentRequest> captor = ArgumentCaptor.forClass(ChangeDepartmentRequest.class);
        verify(adminUserService).changeDepartment(eq(5L), captor.capture());
        assertEquals(20L, captor.getValue().getDepartmentId());
    }

    @Test
    void changeDepartment_departmentIdNull_nghiaLaGoUserRaKhoiDepartment() throws Exception {
        when(adminUserService.changeDepartment(eq(5L), any(ChangeDepartmentRequest.class)))
                .thenReturn(fakeUser(5L, "an", true));

        mockMvc.perform(put(BASE + "/5/department")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":null}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ChangeDepartmentRequest> captor = ArgumentCaptor.forClass(ChangeDepartmentRequest.class);
        verify(adminUserService).changeDepartment(eq(5L), captor.capture());
        assertNull(captor.getValue().getDepartmentId());
    }

    // ================= DELETE =================

    @Test
    void deleteUser_tra204() throws Exception {
        mockMvc.perform(delete(BASE + "/9"))
                .andExpect(status().isNoContent());

        verify(adminUserService).deleteUser(9L);
    }
}