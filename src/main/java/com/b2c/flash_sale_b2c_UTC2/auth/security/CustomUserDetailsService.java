package com.b2c.flash_sale_b2c_UTC2.auth.security;

import com.b2c.flash_sale_b2c_UTC2.user.entity.GroupPermission;
import com.b2c.flash_sale_b2c_UTC2.user.entity.User;
import com.b2c.flash_sale_b2c_UTC2.user.entity.UserRole;
import com.b2c.flash_sale_b2c_UTC2.user.repository.GroupPermissionRepository;
import com.b2c.flash_sale_b2c_UTC2.user.repository.UserRepository;
import com.b2c.flash_sale_b2c_UTC2.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final GroupPermissionRepository groupPermissionRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.redis.core.StringRedisTemplate redisTemplate;

    public CustomUserDetailsService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            GroupPermissionRepository groupPermissionRepository
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.groupPermissionRepository = groupPermissionRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setRedisTemplate(org.springframework.data.redis.core.StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String AUTH_CACHE_PREFIX = "auth:authorities:";
    private static final java.time.Duration AUTH_CACHE_TTL = java.time.Duration.ofSeconds(60);

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        String cacheKey = AUTH_CACHE_PREFIX + user.getId();
        Set<GrantedAuthority> authorities = new HashSet<>();

        if (redisTemplate != null) {
            try {
                Set<String> cached = redisTemplate.opsForSet().members(cacheKey);
                if (cached != null && !cached.isEmpty()) {
                    for (String auth : cached) {
                        authorities.add(new SimpleGrantedAuthority(auth));
                    }
                    return new CustomUserDetails(user, authorities);
                }
            } catch (Exception ex) {
                // Redis error fallback to DB directly
            }
        }

        Set<String> authStringsToCache = new HashSet<>();
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());

        for (UserRole userRole : userRoles) {
            String roleName = userRole.getRole().getName();
            String fullRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
            authorities.add(new SimpleGrantedAuthority(fullRole));
            authStringsToCache.add(fullRole);

            if (userRole.getPermissionGroup() != null) {
                List<GroupPermission> groupPermissions = groupPermissionRepository.findByIdGroupId(userRole.getPermissionGroup().getId());
                for (GroupPermission gp : groupPermissions) {
                    if (Boolean.TRUE.equals(gp.getPermission().getIsActive())) {
                        String code = gp.getPermission().getCode();
                        authorities.add(new SimpleGrantedAuthority(code));
                        authStringsToCache.add(code);
                    }
                }
            }
        }

        if (redisTemplate != null) {
            try {
                if (!authStringsToCache.isEmpty()) {
                    redisTemplate.opsForSet().add(cacheKey, authStringsToCache.toArray(new String[0]));
                    redisTemplate.expire(cacheKey, AUTH_CACHE_TTL);
                }
            } catch (Exception ex) {
                // Redis caching failure is non-fatal
            }
        }

        return new CustomUserDetails(user, authorities);
    }

    public void evictUserAuthorities(Long userId) {
        if (userId != null && redisTemplate != null) {
            try {
                redisTemplate.delete(AUTH_CACHE_PREFIX + userId);
            } catch (Exception ignored) {
            }
        }
    }
}
