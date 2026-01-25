package com.example.neighborhood_library.config;

import com.example.neighborhood_library.domain.AccountRole;
import com.example.neighborhood_library.domain.User;
import com.example.neighborhood_library.domain.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LibraryUserPrincipal implements UserDetails {

    private final Long id;
    private final String login;
    private final String passwordHash;
    private final UserStatus status;
    private final AccountRole role;

    public LibraryUserPrincipal(User user) {
        this.id = user.getId();
        this.login = user.getLogin();
        this.passwordHash = user.getPasswordHash();
        this.status = user.getStatus();
        this.role = user.getAccountRole();
    }

    public Long getId() { return id; }
    public UserStatus getStatus() { return status; }
    public AccountRole getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> auth = new ArrayList<>();
        auth.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        if (status == UserStatus.BANNED) {
            auth.add(new SimpleGrantedAuthority("STATUS_BANNED"));
        }
        return List.copyOf(auth);
    }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return login; }

    // INACTIVE nie może się zalogować (US-003)
    @Override
    public boolean isEnabled() {
        return status != UserStatus.INACTIVE;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }
}
