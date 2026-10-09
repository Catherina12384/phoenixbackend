package com.phoenix.common;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class HttpClientContext implements ClientContext {
    @Override
    public String ip() {
        return read(RequestContextFilter.ATTR_IP, "system");
    }

    @Override
    public String requestId() {
        return read(RequestContextFilter.ATTR_REQUEST_ID, null);
    }

    private static String read(String name, String fallback) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        Object v = attrs == null ? null : attrs.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
        return v instanceof String s ? s : fallback;
    }
}
