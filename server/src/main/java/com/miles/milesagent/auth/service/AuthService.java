package com.miles.milesagent.auth.service;

import com.miles.milesagent.Exception.BusinessException;
import com.miles.milesagent.auth.dto.AuthResponse;
import com.miles.milesagent.auth.dto.LoginRequest;
import com.miles.milesagent.auth.dto.RegisterRequest;
import com.miles.milesagent.auth.mapper.UserMapper;
import com.miles.milesagent.auth.model.User;
import com.miles.milesagent.common.ErrorCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * 邮箱认证服务。
 */
@Service
@Slf4j
public class AuthService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final String VERIFY_PREFIX = "verify:email:";

    private static final String SESSION_USER_ID = "userId";

    private static final Duration VERIFY_TTL = Duration.ofMinutes(5);

    @Resource
    private UserMapper userMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private JavaMailSender javaMailSender;

    @Resource
    private PasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        validateEmail(email);

        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        stringRedisTemplate.opsForValue().set(buildVerifyKey(email), code, VERIFY_TTL);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Miles-Agent 注册验证码");
            message.setText("你的验证码是：" + code + "，5 分钟内有效。");
            javaMailSender.send(message);
        } catch (Exception e) {
            log.error("发送验证码邮件失败: {}", email, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    public void verifyCode(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        validateEmail(email);
        validateCode(rawCode);
        assertCodeMatches(email, rawCode.trim());
    }

    public AuthResponse register(RegisterRequest request, HttpSession session) {
        String email = normalizeEmail(request.getEmail());
        String code = safeTrim(request.getCode());
        String nickname = safeTrim(request.getNickname());
        String password = request.getPassword() == null ? "" : request.getPassword();

        validateEmail(email);
        validateCode(code);
        validateNickname(nickname);
        validatePassword(password);
        assertCodeMatches(email, code);

        if (userMapper.findByEmail(email) != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }

        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setNickname(nickname);
        user.setPasswordHash(passwordEncoder.encode(password));

        try {
            userMapper.insertUser(user);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS, "邮箱已注册");
        }

        stringRedisTemplate.delete(buildVerifyKey(email));
        session.setAttribute(SESSION_USER_ID, user.getId().toString());
        return toAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request, HttpSession session) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword() == null ? "" : request.getPassword();

        validateEmail(email);
        if (!StringUtils.hasText(password)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER_ERROR, "密码不能为空");
        }

        User user = userMapper.findByEmail(email);
        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(401, "邮箱或密码错误");
        }

        session.setAttribute(SESSION_USER_ID, user.getId().toString());
        return toAuthResponse(user);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public AuthResponse me(HttpSession session) {
        Object userId = session.getAttribute(SESSION_USER_ID);
        if (!(userId instanceof String userIdString) || !StringUtils.hasText(userIdString)) {
            throw new BusinessException(401, "未登录");
        }

        return AuthResponse.builder()
                .userId(userIdString)
                .email(session.getAttribute("email") instanceof String value ? value : "")
                .nickname(session.getAttribute("nickname") instanceof String value ? value : "")
                .build();
    }

    public void bindSessionSnapshot(HttpSession session, AuthResponse response) {
        session.setAttribute(SESSION_USER_ID, response.getUserId());
        session.setAttribute("email", response.getEmail());
        session.setAttribute("nickname", response.getNickname());
    }

    private AuthResponse toAuthResponse(User user) {
        return AuthResponse.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .build();
    }

    private String buildVerifyKey(String email) {
        return VERIFY_PREFIX + email;
    }

    private void assertCodeMatches(String email, String code) {
        String cached = stringRedisTemplate.opsForValue().get(buildVerifyKey(email));
        if (!StringUtils.hasText(cached) || !cached.equals(code)) {
            throw new BusinessException(ErrorCode.LOGIN_ERROR_CODE, "验证码错误或已过期");
        }
    }

    private void validateEmail(String email) {
        if (!StringUtils.hasText(email) || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException(ErrorCode.PHONE_EMAIL_ERROR, "邮箱格式不合法");
        }
    }

    private void validateCode(String code) {
        if (!StringUtils.hasText(code) || !code.matches("\\d{6}")) {
            throw new BusinessException(ErrorCode.LOGIN_ERROR_CODE, "验证码错误或已过期");
        }
    }

    private void validateNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER_ERROR, "昵称不能为空");
        }
        if (nickname.length() > 32) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER_ERROR, "昵称长度不能超过 32 个字符");
        }
    }

    private void validatePassword(String password) {
        if (!StringUtils.hasText(password) || password.length() < 8) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER_ERROR, "密码长度不能少于 8 位");
        }
    }

    private String normalizeEmail(String rawEmail) {
        return safeTrim(rawEmail).toLowerCase();
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
