package com.expert.project.noty.jwt;

import com.expert.project.noty.dto.auth.CustomUserDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;

        setFilterProcessesUrl("/auth/login"); // login -> /auth/login으로 url 변경
    }

    // username -> userId parameter
    @Override
    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter("userId");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String userId = null;
        String password = null;

        // Content-Type 확인
        String contentType = request.getHeader("Content-Type");

        try {
            // JSON 요청 처리
            if (contentType != null && contentType.contains("application/json")) {
                // 요청 본문 읽기
                StringBuilder sb = new StringBuilder();
                String line;
                try (BufferedReader reader = request.getReader()) {
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                }

                // JSON 파싱
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(sb.toString());
                userId = rootNode.path("userId").asText();
                password = rootNode.path("password").asText();
            }
            // Form 요청 처리 (기존 방식)
            else {
                userId = obtainUsername(request);
                password = obtainPassword(request);
            }

            // 인증 처리
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userId, password, null);
            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new AuthenticationServiceException("요청 본문을 읽는 중 오류 발생", e);
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) {

        System.out.println("요청 성공");

        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();

        String username = customUserDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();

        String role = auth.getAuthority();

        String token = jwtUtil.createJwt(username, role, 30L * 24 * 60 * 60 * 1000); // 한달

        response.addHeader("Authorization", "Bearer " + token);

        // 응답 본문에도 토큰 포함 (JSON 형식)
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try {
            String tokenJson = "{\"token\":\"" + token + "\", \"userId\":\"" + username + "\", \"role\":\"" + role + "\"}";
            response.getWriter().write(tokenJson);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) {

        System.out.println("요청 실패");

        // 로그인 실패시 401 에러
        response.setStatus(401);
    }
}