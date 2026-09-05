package com.vegetableshop.service;

import com.vegetableshop.entity.User;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Profile("mysql")
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate = new OidcUserService();
    private final UserService userService;

    public GoogleOidcUserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw authenticationError("Google chưa xác minh địa chỉ email này");
        }

        try {
            User user = userService.upsertGoogleUser(
                oidcUser.getSubject(),
                oidcUser.getEmail(),
                oidcUser.getFullName()
            );
            Set<GrantedAuthority> authorities = Set.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
            );
            return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                "email"
            );
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw authenticationError(exception.getMessage());
        }
    }

    private OAuth2AuthenticationException authenticationError(String message) {
        return new OAuth2AuthenticationException(
            new OAuth2Error("google_login_failed"),
            message
        );
    }
}
