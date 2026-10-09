package com.phoenix.otp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.phoenix.common.Masks;
import com.phoenix.exception.OtpDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Delivers an OTP that WE generated through MSG91's Send OTP API (POST /api/v5/otp?otp=...).
 * MSG91 is only the courier: we never call its verify endpoint. One attempt, short timeout,
 * no retry (a retry could deliver two SMS). The OTP is in the request URL, so URLs and exception
 * messages from the HTTP client are never logged.
 */
@Component
@ConditionalOnProperty(name = "app.otp.sender", havingValue = "msg91", matchIfMissing = true)
public class Msg91OtpSender implements OtpSender {
    private static final Logger log = LoggerFactory.getLogger(Msg91OtpSender.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Msg91Response(String type, String message) {}

    private final OtpProperties.Msg91 cfg;
    private final long expiryMinutes;
    private final RestClient client;

    public Msg91OtpSender(OtpProperties props) {
        this.cfg = props.msg91();
        if (isBlank(cfg.authKey()) || isBlank(cfg.templateId())) {
            throw new IllegalStateException(
                    "MSG91_AUTH_KEY and MSG91_TEMPLATE_ID must be set when OTP_SENDER=msg91 (the default)");
        }
        this.expiryMinutes = Math.max(1, props.ttl().toMinutes());
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) cfg.timeout().toMillis());
        factory.setReadTimeout((int) cfg.timeout().toMillis());
        this.client = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public void send(String phoneE164, String otp, OtpPurpose purpose) {
        URI uri = UriComponentsBuilder.fromUriString(cfg.url())
                .queryParam("template_id", cfg.templateId())
                .queryParam("mobile", phoneE164.substring(1))          // MSG91 wants 91XXXXXXXXXX
                .queryParam("otp", otp)
                .queryParam("otp_expiry", expiryMinutes)
                .build().toUri();
        try {
            Msg91Response r = client.post().uri(uri)
                    .header("authkey", cfg.authKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .body(Msg91Response.class);
            if (r == null || !"success".equalsIgnoreCase(r.type())) {
                log.error("MSG91 rejected OTP request for {}: {}", Masks.phone(phoneE164), r == null ? "empty response" : r.message());
                throw new OtpDeliveryException("SMS provider rejected the request");
            }
            log.info("MSG91 accepted OTP ({}) for {}", purpose, Masks.phone(phoneE164));
        } catch (RestClientException e) {
            // Class name only: the exception message can contain the URL (and therefore the OTP).
            log.error("MSG91 call failed for {}: {}", Masks.phone(phoneE164), e.getClass().getSimpleName());
            throw new OtpDeliveryException("SMS provider unavailable");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
