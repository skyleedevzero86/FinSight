package com.sleekydz86.finsight.core.user.domain.port.out.dto;

import com.sleekydz86.finsight.core.user.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsDto {
    private UserStatus status;
    private Long count;
}