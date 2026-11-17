package com.enterprise.aiassistant.backend.auth.controller;

import com.enterprise.aiassistant.backend.auth.config.JwtProperties;
import com.enterprise.aiassistant.backend.auth.dto.request.LoginRequest;
import com.enterprise.aiassistant.backend.auth.dto.request.RegisterRequest;
import com.enterprise.aiassistant.backend.auth.dto.response.AuthResponse;
import com.enterprise.aiassistant.backend.auth.dto.response.AuthResult;
import com.enterprise.aiassistant.backend.auth.service.AuthService;
import com.enterprise.aiassistant.backend.common.exception.ErrorCode;
import com.enterprise.aiassistant.backend.common.exception.GlobalExceptionHandler;
import com.enterprise.aiassistant.backend.common.exception.business_exception.AuthenticationException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.enterprise\\.aiassistant\\.backend\\.auth\\.security\\..*"
        )
)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthControllerTest.TestConfig.class})
class AuthControllerTest {

    private static final String BASE = "/api/v1/auth";

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtProperties jwtProperties() {
            JwtProperties props = new JwtProperties();
            JwtProperties.Token refresh = new JwtProperties.Token();
            refresh.setSecret("test-secret");
            refresh.setExpiration(604_800_000L);
            props.setRefreshToken(refresh);
            return props;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Giá trị thực mà controller đang dùng (có thể bị application.yml bind đè)
    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private AuthService authService;

    // ---------- helper ----------

    private AuthResult fakeResult(String access, String refresh) {
        AuthResponse body = AuthResponse.builder()
                .accessToken(access)
                .tokenType("Bearer")
                .accessTokenExpiresIn(900)
                .refreshTokenExpiresIn(604_800)
                .build();
        return new AuthResult(body, refresh);
    }

    private long expectedMaxAgeSeconds() {
        return Duration.ofMillis(jwtProperties.getRefreshToken().getExpiration()).toSeconds();
    }

    // ================= LOGIN =================

    @Test
    void login_hopLe_tra200_bodyKhongChuaRefreshToken_vaSetCookie() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .userName("huy")
                .password("12345678")
                .build();
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(fakeResult("access-abc", "refresh-xyz"));

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-abc"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-xyz")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/api/v1/auth")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=" + expectedMaxAgeSeconds())));
    }

    @Test
    void login_thieuUserName_tra400() throws Exception {
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"12345678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value(containsString("userName")));

        verifyNoInteractions(authService);
    }

    @Test
    void login_matKhauNganHon8KyTu_tra400() throws Exception {
        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"huy\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("password")));

        verifyNoInteractions(authService);
    }

    @Test
    void login_serviceNemAuthenticationException_traStatusTheoErrorCode() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new AuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post(BASE + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userName\":\"huy\",\"password\":\"saimatkhau\"}"))
                .andExpect(status().is(ErrorCode.INVALID_REFRESH_TOKEN.getStatus().value()))
                .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"))
                .andExpect(jsonPath("$.path").value(BASE + "/login"));
    }

    // ================= REGISTER =================

    @Test
    void register_hopLe_tra201_vaSetCookie() throws Exception {
        String json = """
                {"fullName":"Nguyen Van Huy","userName":"huy","password":"12345678","email":"huy@mail.com"}
                """;
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(fakeResult("access-abc", "refresh-xyz"));

        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-abc"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=refresh-xyz")));
    }

    @Test
    void register_bodyRong_tra400() throws Exception {
        mockMvc.perform(post(BASE + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    // ================= REFRESH TOKEN =================

    @Test
    void refreshToken_coCookie_tra200_vaCapCookieMoi() throws Exception {
        when(authService.refreshToken("old-refresh"))
                .thenReturn(fakeResult("new-access", "new-refresh"));

        mockMvc.perform(post(BASE + "/refresh-token")
                        .cookie(new Cookie("refreshToken", "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=new-refresh")));

        verify(authService).refreshToken("old-refresh");
    }

    @Test
    void refreshToken_khongCoCookie_traLoiVaKhongGoiService() throws Exception {
        mockMvc.perform(post(BASE + "/refresh-token"))
                .andExpect(status().is(ErrorCode.INVALID_REFRESH_TOKEN.getStatus().value()))
                .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));

        verifyNoInteractions(authService);
    }

    @Test
    void refreshToken_tokenKhongHopLe_traLoiTheoErrorCode() throws Exception {
        when(authService.refreshToken("bad"))
                .thenThrow(new AuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN));

        mockMvc.perform(post(BASE + "/refresh-token")
                        .cookie(new Cookie("refreshToken", "bad")))
                .andExpect(status().is(ErrorCode.INVALID_REFRESH_TOKEN.getStatus().value()));
    }

    // ================= LOGOUT =================

    @Test
    void logout_coCookie_tra204_goiService_vaXoaCookie() throws Exception {
        mockMvc.perform(post(BASE + "/logout")
                        .cookie(new Cookie("refreshToken", "some-refresh")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=;")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logout("some-refresh");
    }

    @Test
    void logout_tokenDaVoHieu_vanTra204() throws Exception {
        doThrow(new AuthenticationException(ErrorCode.INVALID_REFRESH_TOKEN))
                .when(authService).logout("expired");

        mockMvc.perform(post(BASE + "/logout")
                        .cookie(new Cookie("refreshToken", "expired")))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void logout_khongCoCookie_van204_khongGoiService() throws Exception {
        mockMvc.perform(post(BASE + "/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verifyNoInteractions(authService);
    }
}