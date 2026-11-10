package com.enterprise.aiassistant.backend.auth.security;

import com.enterprise.aiassistant.backend.auth.service.CustomUserDetailsService;
import com.enterprise.aiassistant.backend.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String username = jwtService.extractAccessUsername(token);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                if (jwtService.isAccessTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (JwtException | AuthenticationException
                 | com.enterprise.aiassistant.backend.common.exception.business_exception.AuthenticationException
                 | IllegalArgumentException ex) {
            // Token không dùng được thì coi như chưa đăng nhập:
            // - AuthenticationException của app: JwtService ném khi token sai hoặc hết hạn
            // - IllegalArgumentException: JJWT ném khi token rỗng (header "Bearer ")
            // - AuthenticationException của Spring: loadUserByUsername không tìm thấy user
            // Không throw ở đây (lỗi thoát khỏi filter sẽ thành 500). Xóa context rồi cho request
            // đi tiếp, endpoint yêu cầu authenticated() sẽ bị Spring Security chặn và trả 401.
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}