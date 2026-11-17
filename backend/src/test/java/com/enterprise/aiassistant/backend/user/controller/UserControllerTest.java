package com.enterprise.aiassistant.backend.user.controller;

import com.enterprise.aiassistant.backend.common.exception.GlobalExceptionHandler;
import com.enterprise.aiassistant.backend.user.dto.request.UpdateProfileRequest;
import com.enterprise.aiassistant.backend.user.dto.response.UserResponse;
import com.enterprise.aiassistant.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.enterprise\\.aiassistant\\.backend\\.auth\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    private static final String URL = "/api/v1/users/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    // ---------- helper ----------

    private UserResponse fakeUser(String fullName, String email) {
        return UserResponse.builder()
                .id(1L)
                .username("huy")
                .email(email)
                .fullName(fullName)
                .role("USER")
                .permissions(Set.of("DOCUMENT_READ", "CHAT_USE"))
                .departmentId(10L)
                .departmentName("Engineering")
                .enabled(true)
                .createdAt(LocalDateTime.of(2026, 1, 15, 9, 30))
                .build();
    }

    // ================= GET /me =================

    @Test
    @WithMockUser(username = "huy")
    void getMe_traVeDayDuThongTinUserDangNhap() throws Exception {
        when(userService.getCurrentUser("huy"))
                .thenReturn(fakeUser("Nguyen Van Huy", "huy@mail.com"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("huy"))
                .andExpect(jsonPath("$.email").value("huy@mail.com"))
                .andExpect(jsonPath("$.fullName").value("Nguyen Van Huy"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.permissions",
                        containsInAnyOrder("DOCUMENT_READ", "CHAT_USE")))
                .andExpect(jsonPath("$.departmentId").value(10))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.createdAt").exists());

        // username lấy từ UserDetails (người đăng nhập), không phải từ request
        verify(userService).getCurrentUser("huy");
    }

    @Test
    @WithMockUser(username = "an")
    void getMe_dungUsernameCuaNguoiDangNhap() throws Exception {
        when(userService.getCurrentUser("an"))
                .thenReturn(fakeUser("Tran Van An", "an@mail.com"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk());

        verify(userService).getCurrentUser("an");
        verify(userService, never()).getCurrentUser("huy");
    }

    // ================= PUT /me =================

    @Test
    @WithMockUser(username = "huy")
    void updateMe_hopLe_tra200_vaGoiServiceVoiDungUsername() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Ten Moi")
                .email("moi@mail.com")
                .build();
        when(userService.updateProfile(eq("huy"), any(UpdateProfileRequest.class)))
                .thenReturn(fakeUser("Ten Moi", "moi@mail.com"));

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ten Moi"))
                .andExpect(jsonPath("$.email").value("moi@mail.com"));

        verify(userService).updateProfile(eq("huy"), any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "huy")
    void updateMe_chiCoFullName_vanHopLeVi_emailKhongBatBuoc() throws Exception {
        when(userService.updateProfile(eq("huy"), any(UpdateProfileRequest.class)))
                .thenReturn(fakeUser("Ten Moi", "huy@mail.com"));

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ten Moi\"}"))
                .andExpect(status().isOk());

        verify(userService).updateProfile(eq("huy"), any(UpdateProfileRequest.class));
    }

    @Test
    @WithMockUser(username = "huy")
    void updateMe_fullNameRong_tra400_vaKhongGoiService() throws Exception {
        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"huy@mail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("fullName")))
                .andExpect(jsonPath("$.message").value(containsString("Họ tên không được để trống")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "huy")
    void updateMe_thieuFullName_tra400() throws Exception {
        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"huy@mail.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("fullName")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "huy")
    void updateMe_emailSaiDinhDang_tra400_vaKhongGoiService() throws Exception {
        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ten Moi\",\"email\":\"khong-phai-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("email")))
                .andExpect(jsonPath("$.message").value(containsString("Email không hợp lệ")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "huy")
    void updateMe_fullNameDaiHon100KyTu_tra400() throws Exception {
        String tooLong = "a".repeat(101);

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("fullName")));

        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(username = "huy")
    void updateMe_khongChoSuaUsername_truongThuaBiBoQua() throws Exception {
        // Gửi thêm "username" và "role": DTO không có field này nên không thể bị sửa
        when(userService.updateProfile(eq("huy"), any(UpdateProfileRequest.class)))
                .thenReturn(fakeUser("Ten Moi", "huy@mail.com"));

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ten Moi\",\"username\":\"hacker\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("huy"))
                .andExpect(jsonPath("$.role").value("USER"));

        // service luôn nhận username từ người đăng nhập, không phải từ body
        verify(userService).updateProfile(eq("huy"), any(UpdateProfileRequest.class));
        verify(userService, never()).updateProfile(eq("hacker"), any());
    }
}