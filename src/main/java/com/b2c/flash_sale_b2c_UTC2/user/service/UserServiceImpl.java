package com.b2c.flash_sale_b2c_UTC2.user.service;

import com.b2c.flash_sale_b2c_UTC2.auth.exception.AuthErrorCode;
import com.b2c.flash_sale_b2c_UTC2.common.exception.BusinessException;
import com.b2c.flash_sale_b2c_UTC2.user.dto.ChangePasswordRequest;
import com.b2c.flash_sale_b2c_UTC2.user.dto.UpdateProfileRequest;
import com.b2c.flash_sale_b2c_UTC2.user.dto.UserResponse;
import com.b2c.flash_sale_b2c_UTC2.user.entity.User;
import com.b2c.flash_sale_b2c_UTC2.user.exception.UserErrorCode;
import com.b2c.flash_sale_b2c_UTC2.user.mapper.UserMapper;
import com.b2c.flash_sale_b2c_UTC2.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final org.springframework.data.redis.core.StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String trimmedPhone = request.getPhone().trim();
            userRepository.findByPhone(trimmedPhone).ifPresent(existing -> {
                if (!existing.getId().equals(userId)) {
                    throw new BusinessException(AuthErrorCode.PHONE_ALREADY_EXISTS);
                }
            });
            user.setPhone(trimmedPhone);
        }

        user.setFullName(request.getFullName().trim());
        user.setUpdatedAt(Instant.now());

        user = userRepository.save(user);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(UserErrorCode.OLD_PASSWORD_INCORRECT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Thu hồi toàn bộ refresh token của user trong Redis
        revokeAllUserRefreshTokens(userId);
    }

    private void revokeAllUserRefreshTokens(Long userId) {
        try {
            String userTokensKey = "auth:user_tokens:" + userId;
            java.util.Set<String> jtis = stringRedisTemplate.opsForSet().members(userTokensKey);
            if (jtis != null && !jtis.isEmpty()) {
                java.util.List<String> keys = jtis.stream()
                        .map(jti -> "auth:refresh:" + userId + ":" + jti)
                        .toList();
                stringRedisTemplate.delete(keys);
            }
            stringRedisTemplate.delete(userTokensKey);
            log.info("Đã thu hồi toàn bộ Refresh Tokens trong Redis cho userId={}", userId);
        } catch (Exception ex) {
            log.warn("Lỗi khi thu hồi refresh tokens trong Redis cho userId={}: {}", userId, ex.getMessage());
        }
    }
}
