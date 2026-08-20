package com.carpool.security;

import com.carpool.config.AppProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> uploadBuckets = new ConcurrentHashMap<>();
    private final int authLimit;
    private final int uploadLimit;

    public RateLimitFilter(AppProperties appProperties) {
        this.authLimit = appProperties.getSecurity().getRateLimit().getAuthPerMinute();
        this.uploadLimit = appProperties.getSecurity().getRateLimit().getUploadPerMinute();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // Skip rate limiting for CORS preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        String key = request.getRemoteAddr() + "|" + path;

        if (path.startsWith("/api/auth") && !consume(authBuckets, key, authLimit)) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Rate limit exceeded");
            return;
        }
        if (path.startsWith("/api/owners") && "multipart/form-data".equalsIgnoreCase(request.getContentType() == null ? "" : request.getContentType().split(";")[0])
            && !consume(uploadBuckets, key, uploadLimit)) {
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(), "Upload rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean consume(Map<String, Bucket> map, String key, int capacity) {
        Bucket bucket = map.computeIfAbsent(key,
            k -> Bucket.builder().addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, Duration.ofMinutes(1)).build()).build());
        return bucket.tryConsume(1);
    }
}
