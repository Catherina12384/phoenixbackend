package com.phoenix.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/** First filter in the chain: assigns a request id, resolves the client IP, fills the log MDC. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {
    public static final String ATTR_IP = "phoenix.ip";
    public static final String ATTR_REQUEST_ID = "phoenix.requestId";
    private static final Logger log = LoggerFactory.getLogger(RequestContextFilter.class);
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    private final ClientIpResolver ipResolver;

    public RequestContextFilter(ClientIpResolver ipResolver) {
        this.ipResolver = ipResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String inbound = req.getHeader("X-Request-Id");
        String id = inbound != null && SAFE_ID.matcher(inbound).matches() ? inbound : UUID.randomUUID().toString();
        String ip = ipResolver.resolve(req);

        req.setAttribute(ATTR_REQUEST_ID, id);
        req.setAttribute(ATTR_IP, ip);
        MDC.put("requestId", id);
        MDC.put("ip", ip);
        res.setHeader("X-Request-Id", id);

        long start = System.nanoTime();
        try {
            chain.doFilter(req, res);
        } finally {
            // Path only: query strings are never logged.
            log.info("{} {} -> {} ({} ms)", req.getMethod(), req.getRequestURI(), res.getStatus(),
                    (System.nanoTime() - start) / 1_000_000);
            MDC.clear();
        }
    }
}
