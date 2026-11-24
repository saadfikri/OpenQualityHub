package com.openqualityhub.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPassThroughWhenNoToken() throws ServletException, IOException {
        // No Authorization header
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldPassThroughWhenNoBearer() throws ServletException, IOException {
        request.addHeader("Authorization", "Basic someCredentials");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldPassThroughWhenEmptyBearer() throws ServletException, IOException {
        request.addHeader("Authorization", "Bearer ");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // jwtService.isTokenValid("") may or may not be called depending on empty-string check
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateValidToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "user@example.com";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.isTokenValid(token)).thenReturn(true);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("email", email);
        Claims claims = new DefaultClaims(claimsMap);
        when(jwtService.extractClaims(token)).thenReturn(claims);

        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("TEAM_READ")))
            .when(userPrincipal).getAuthorities();
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userPrincipal);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(userPrincipal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void shouldSkipWhenInvalidToken() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.isTokenValid(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void shouldSkipWhenExpiredToken() throws ServletException, IOException {
        String token = "expired.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.isTokenValid(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldSetSecurityContextOnValidToken() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "admin@example.com";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.isTokenValid(token)).thenReturn(true);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("email", email);
        Claims claims = new DefaultClaims(claimsMap);
        when(jwtService.extractClaims(token)).thenReturn(claims);

        UserPrincipal userPrincipal = mock(UserPrincipal.class);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ADMIN")))
            .when(userPrincipal).getAuthorities();
        when(userDetailsService.loadUserByUsername(email)).thenReturn(userPrincipal);

        filter.doFilterInternal(request, response, filterChain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(userPrincipal, auth.getPrincipal());
        assertNull(auth.getCredentials());
        assertFalse(auth.getAuthorities().isEmpty());
    }

    @Test
    void shouldContinueFilterChainEvenWhenUserDetailsServiceThrows() throws ServletException, IOException {
        String token = "valid.jwt.token";
        String email = "missing@example.com";

        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.isTokenValid(token)).thenReturn(true);

        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("email", email);
        Claims claims = new DefaultClaims(claimsMap);
        when(jwtService.extractClaims(token)).thenReturn(claims);

        when(userDetailsService.loadUserByUsername(email))
            .thenThrow(new RuntimeException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void shouldAlwaysCallFilterChain() throws ServletException, IOException {
        // With valid token
        String token = "any.token";
        request.addHeader("Authorization", "Bearer " + token);
        when(jwtService.isTokenValid(token)).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
