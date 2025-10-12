package com.gomokumatching.security;

import com.gomokumatching.model.Player;
import com.gomokumatching.model.enums.AccountStatusEnum;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * Custom UserDetails implementation for Spring Security.
 *
 * Security Features:
 * - Immutable user principal
 * - Role-based authority mapping
 * - Account status checks (locked, expired, enabled)
 * - Clean separation from entity layer
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean isActive;
    private final AccountStatusEnum accountStatus;
    private final Collection<? extends GrantedAuthority> authorities;

    /**
     * Create UserDetails from Player entity.
     *
     * @param player Player entity
     */
    public CustomUserDetails(Player player) {
        this.id = player.getPlayerId();
        this.username = player.getUsername();
        this.email = player.getEmail();
        this.password = player.getPasswordHash();
        this.isActive = player.isActive();
        this.accountStatus = player.getAccountStatus();

        // now we have a default role for each user
        // TODO: add support for multiple roles
        this.authorities = Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_USER")
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    /**
     * Account is non-expired if active and not deleted.
     */
    @Override
    public boolean isAccountNonExpired() {
        return isActive && accountStatus != AccountStatusEnum.DELETED;
    }

    /**
     * Account is non-locked if not suspended.
     */
    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != AccountStatusEnum.SUSPENDED;
    }

    /**
     * Credentials never expire in this implementation.
     * Can be extended later for password expiration policies.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Account is enabled if active and status is ACTIVE.
     */
    @Override
    public boolean isEnabled() {
        return isActive && accountStatus == AccountStatusEnum.ACTIVE;
    }
}
