package com.vegetableshop.service;

import com.vegetableshop.entity.User;
import com.vegetableshop.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Profile("mysql")
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final String oauthOnlyPassword;

    public DatabaseUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.oauthOnlyPassword = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
            .orElseThrow(() -> new UsernameNotFoundException("Email hoặc mật khẩu không đúng"));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPassword() == null ? oauthOnlyPassword : user.getPassword())
            .roles(user.getRole().name())
            .disabled(!user.isStatus() || !user.isEmailVerified())
            .build();
    }
}
