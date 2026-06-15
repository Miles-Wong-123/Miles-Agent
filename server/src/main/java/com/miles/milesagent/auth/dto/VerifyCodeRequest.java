package com.miles.milesagent.auth.dto;

import lombok.Data;

@Data
public class VerifyCodeRequest {

    private String email;

    private String code;
}
