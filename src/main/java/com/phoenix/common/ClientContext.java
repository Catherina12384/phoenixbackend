package com.phoenix.common;

/** Per-request facts (client IP, request id) without tying services to the servlet API. */
public interface ClientContext {
    String ip();

    String requestId();
}
