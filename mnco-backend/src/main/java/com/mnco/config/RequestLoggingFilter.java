package com.mnco.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Emits one structured JSON log line per HTTP request/response (FR-MO-03).
 *
 * Format: { timestamp, level, traceId, httpMethod, requestUri,
 *            userId, httpStatus, durationMs, errorMessage? }
 *
 * traceId is generated per request and placed in the MDC so async operations
 * (e.g. audit log writes) can correlate back to the originating request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        long startMs = System.currentTimeMillis();

        // Set traceId in response header for client-side correlation
        response.setHeader("X-Trace-Id", traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startMs;
            emitStructuredLog(request, response, traceId, durationMs);
        }
    }

    private void emitStructuredLog(HttpServletRequest request, HttpServletResponse response,
                                   String traceId, long durationMs) {
        try {
            String userId = resolveUserId();
            int status = response.getStatus();
            boolean isError = status >= 400;

            Map<String, Object> logEntry = new LinkedHashMap<>();
            logEntry.put("timestamp",  Instant.now().toString());
            logEntry.put("level",      isError ? "WARN" : "INFO");
            logEntry.put("traceId",    traceId);
            logEntry.put("httpMethod", request.getMethod());
            logEntry.put("requestUri", request.getRequestURI());
            logEntry.put("userId",     userId);
            logEntry.put("httpStatus", status);
            logEntry.put("durationMs", durationMs);

            if (isError) {
                logEntry.put("errorMessage", "HTTP " + status);
            }

            // Write as a single JSON line — parseable by Loki/ELK without regex
            log.info(objectMapper.writeValueAsString(logEntry));
        } catch (Exception ex) {
            log.warn("Structured log emission failed: {}", ex.getMessage());
        }
    }

    private String resolveUserId() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                return auth.getName();
            }
        } catch (Exception ignored) {}
        return "anonymous";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip logging for health checks — they're noisy and not useful
        String path = request.getServletPath();
        return path.startsWith("/actuator/health") || path.startsWith("/actuator/info");
    }
}
