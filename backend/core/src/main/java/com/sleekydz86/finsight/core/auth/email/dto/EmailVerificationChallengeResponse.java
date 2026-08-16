package com.sleekydz86.finsight.core.auth.email.dto;

import com.sleekydz86.finsight.core.auth.email.EmailVerificationPurpose;
import com.sleekydz86.finsight.core.auth.email.EmailVerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationChallengeResponse {

    private String maskedEmail;
    private EmailVerificationPurpose purpose;
    private String purposeLabel;
    private EmailVerificationStatus status;
    private int expiresInSeconds;
    private boolean expired;
}
