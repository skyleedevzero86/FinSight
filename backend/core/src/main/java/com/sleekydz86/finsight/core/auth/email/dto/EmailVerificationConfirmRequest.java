package com.sleekydz86.finsight.core.auth.email.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailVerificationConfirmRequest {

    @NotBlank(message = "인증 토큰이 필요합니다")
    private String token;

    @NotBlank(message = "인증 코드를 입력해 주세요")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 6자리 숫자입니다")
    private String code;
}
