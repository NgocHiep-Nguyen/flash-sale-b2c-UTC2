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
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final GroupPermissionRepository groupPermissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));

        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getId());
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (UserRole userRole : userRoles) {
            String roleName = userRole.getRole().getName();
            if (!roleName.startsWith("ROLE_")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            } else {
                authorities.add(new SimpleGrantedAuthority(roleName));
            }

            if (userRole.getPermissionGroup() != null) {
                List<GroupPermission> groupPermissions = groupPermissionRepository.findByIdGroupId(userRole.getPermissionGroup().getId());
                for (GroupPermission gp : groupPermissions) {
                    if (Boolean.TRUE.equals(gp.getPermission().getIsActive())) {
                        authorities.add(new SimpleGrantedAuthority(gp.getPermission().getCode()));
                    }
                }
            }
        }

        return new CustomUserDetails(user, authorities);
    }
}
