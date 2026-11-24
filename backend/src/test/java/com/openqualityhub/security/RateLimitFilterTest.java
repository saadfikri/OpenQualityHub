package com.openqualityhub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(3); // Low limit for testing
    }

    @Test
    void nonAuthPath_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/projects");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void authPath_underLimit_shouldPassThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void authPath_overLimit_shouldReturn429() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Exhaust the limit (3 requests)
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // 4th request should be rate-limited
        when(response.getWriter()).thenReturn(pw);
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
        String body = sw.toString();
        assertTrue(body.contains("Rate limit exceeded"));
    }

    @Test
    void xForwardedFor_shouldExtractFirstIp() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void differentIps_shouldHaveSeparateLimits() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        // Exhaust limit for IP A
        when(request.getRemoteAddr()).thenReturn("10.0.0.10");
        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // IP B should still be allowed
        when(request.getRemoteAddr()).thenReturn("10.0.0.20");
        filter.doFilterInternal(request, response, filterChain);

        // filterChain should have been called 4 times (3 for IP A + 1 for IP B)
        verify(filterChain, times(4)).doFilter(request, response);
    }

    @Test
    void refreshPath_shouldAlsoBeRateLimited() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/v1/auth/refresh");
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        for (int i = 0; i < 3; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        when(response.getWriter()).thenReturn(pw);
        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(429);
    }
}
