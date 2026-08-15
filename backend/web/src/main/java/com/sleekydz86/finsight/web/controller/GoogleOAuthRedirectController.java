package com.sleekydz86.finsight.web.controller;

import com.sleekydz86.finsight.core.auth.config.GoogleOAuthProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class GoogleOAuthRedirectController {

    private final GoogleOAuthProperties googleOAuthProperties;

    public GoogleOAuthRedirectController(GoogleOAuthProperties googleOAuthProperties) {
        this.googleOAuthProperties = googleOAuthProperties;
    }

    @GetMapping("/login/oauth2/code/google")
    public ResponseEntity<Void> redirectToFrontend(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(value = "error_description", required = false) String errorDescription) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(googleOAuthProperties.getFrontendCallbackUri());
        if (error != null && !error.isBlank()) {
            builder.queryParam("error", error);
        }
        if (errorDescription != null && !errorDescription.isBlank()) {
            builder.queryParam("error_description", errorDescription);
        }
        if (code != null && !code.isBlank()) {
            builder.queryParam("code", code);
        }
        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(builder.encode().build().toUri())
                .build();
    }
}
