package com.sleekydz86.finsight.core.user.domain.port.in.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPasswordChangeRequest {

    @NotBlank
    private String oldPassword;

    @NotBlank
    @Size(min = 8, max = 20)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>_+=\\-\\[\\]\\\\;'`~])[A-Za-z\\d!@#$%^&*(),.?\":{}|<>_+=\\-\\[\\]\\\\;'`~]{8,}$")
    private String newPassword;

    @NotBlank
    private String newPasswordConfirm;
}