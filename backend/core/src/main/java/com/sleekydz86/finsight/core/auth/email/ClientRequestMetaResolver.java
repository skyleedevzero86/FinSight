package com.sleekydz86.finsight.core.auth.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

@Component
public class ClientRequestMetaResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientRequestMetaResolver.class);
    private static final Pattern PRIVATE_IP = Pattern.compile(
            "^(127\\.|10\\.|192\\.168\\.|172\\.(1[6-9]|2[0-9]|3[0-1])\\.|0:0:0:0:0:0:0:1|::1|localhost).*$",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    public ClientRequestMetaResolver(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String resolveClientIp(HttpServletRequest request) {
        String forwarded = firstIp(request.getHeader("X-Forwarded-For"));
        if (forwarded != null) {
            return forwarded;
        }
        String realIp = trimToNull(request.getHeader("X-Real-IP"));
        if (realIp != null) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    public String resolveUserAgent(HttpServletRequest request) {
        return trimToNull(request.getHeader("User-Agent"));
    }

    public String resolveLocation(String ip) {
        if (ip == null || ip.isBlank() || PRIVATE_IP.matcher(ip).matches()) {
            return "로컬 환경 " + (ip == null ? "" : ip).trim();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://ip-api.com/json/" + ip + "?lang=ko&fields=status,country,regionName,city,query"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            GeoResponse response = objectMapper.readValue(httpResponse.body(), GeoResponse.class);
            if (response == null || !"success".equalsIgnoreCase(response.getStatus())) {
                return ip;
            }
            String city = firstNonBlank(response.getCity(), response.getRegionName());
            String country = firstNonBlank(response.getCountry(), "");
            if (city != null && !country.isBlank()) {
                return city + ", " + country + " " + ip;
            }
            if (!country.isBlank()) {
                return country + " " + ip;
            }
            return ip;
        } catch (Exception e) {
            log.debug("IP 위치 조회 실패: {}", e.getMessage());
            return ip;
        }
    }

    private String firstIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }
        String first = forwardedFor.split(",")[0].trim();
        return first.isEmpty() ? null : first;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
