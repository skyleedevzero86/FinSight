package com.sleekydz86.finsight.core.auth.adapter.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverProfileEnvelope {

    private String resultcode;
    private String message;
    private NaverProfileResponse response;

    public String getResultcode() {
        return resultcode;
    }

    public void setResultcode(String resultcode) {
        this.resultcode = resultcode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public NaverProfileResponse getResponse() {
        return response;
    }

    public void setResponse(NaverProfileResponse response) {
        this.response = response;
    }
}
