package com.miles.milesagent.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 登录后返回给前端的用户摘要。
 */
@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String userId;

    private String email;

    private String nickname;
}
