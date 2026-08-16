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
public class EmailVerificationConfirmResponse {

    private boolean verified;
    private EmailVerificationPurpose purpose;
    private String purposeLabel;
    private String maskedEmail;
    private String username;
    private String redirectTo;
    private boolean canResetPassword;
    private String email;
}
