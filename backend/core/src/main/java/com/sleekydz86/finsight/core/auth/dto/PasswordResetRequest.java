package com.sleekydz86.finsight.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetRequest {

    @NotBlank(message = "복구 토큰은 필수입니다")
    private String recoveryToken;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String confirmPassword;
}