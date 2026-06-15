package com.miles.milesagent.auth.model;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 用户实体。
 * 对应 PostgreSQL 中的 users 表。
 */
@Data
public class User {

    private String id;

    private String email;

    private String nickname;

    private String passwordHash;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
