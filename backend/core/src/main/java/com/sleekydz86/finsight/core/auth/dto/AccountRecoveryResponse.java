package com.sleekydz86.finsight.core.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRecoveryResponse {

    private boolean success;
    private String message;
    private String recoveryToken;
    private long expiresIn;
}