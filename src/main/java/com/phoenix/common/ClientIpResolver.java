package com.phoenix.common;

import com.phoenix.config.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Uses the socket address. X-Forwarded-For is honoured ONLY when the socket peer is one of the
 * configured trusted proxies; otherwise a client could spoof its IP and dodge the IP rate limits.
 */
@Component
public class ClientIpResolver {
    private static final Pattern IP_LIKE = Pattern.compile("[0-9a-fA-F:.]{1,45}");
    private final Set<String> trusted;

    public ClientIpResolver(SecurityProperties props) {
        this.trusted = Set.copyOf(props.trustedProxies());
    }

    public String resolve(HttpServletRequest req) {
        String remote = req.getRemoteAddr();
        if (!trusted.contains(remote)) return remote;
        String xff = req.getHeader("X-Forwarded-For");
        if (xff == null || xff.isBlank()) return remote;
        String[] parts = xff.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {          // right-most entry not added by a trusted proxy
            String ip = parts[i].trim();
            if (!ip.isEmpty() && !trusted.contains(ip)) return IP_LIKE.matcher(ip).matches() ? ip : remote;
        }
        return remote;
    }
}
