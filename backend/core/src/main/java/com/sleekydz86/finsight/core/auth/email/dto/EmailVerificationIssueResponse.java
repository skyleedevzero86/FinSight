package com.sleekydz86.finsight.core.auth.email.dto;

import com.sleekydz86.finsight.core.auth.email.EmailVerificationPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationIssueResponse {

    private String challengeToken;
    private String maskedEmail;
    private EmailVerificationPurpose purpose;
    private String purposeLabel;
    private int expiresInSeconds;
}
