package com.university.auth.security;

import com.university.auth.entity.Permission;
import com.university.auth.entity.Role;
import com.university.auth.entity.User;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final boolean active;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.username = user.getEmail();
        this.password = user.getPassword();
        this.active = user.isActive();
        this.roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        this.permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        Set<GrantedAuthority> auths = new HashSet<>();
        roles.forEach(role -> auths.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> auths.add(new SimpleGrantedAuthority(permission)));
        this.authorities = auths;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
