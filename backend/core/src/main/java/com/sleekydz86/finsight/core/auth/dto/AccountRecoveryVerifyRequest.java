package com.sleekydz86.finsight.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRecoveryVerifyRequest {

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "사용자명은 필수입니다")
    private String username;

    @NotBlank(message = "OTP 코드는 필수입니다")
    @Pattern(regexp = "\\d{6}", message = "OTP 코드는 6자리 숫자여야 합니다")
    private String otpCode;
}