package com.vegetableshop.service;

import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.entity.Role;
import com.vegetableshop.entity.User;
import com.vegetableshop.exception.DuplicateEmailException;
import com.vegetableshop.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Profile("mysql")
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng có id: " + id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
            .filter(User::isStatus)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản đang hoạt động"));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return email != null && userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    @Transactional
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("Email này đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setAddress(normalizeOptional(request.getAddress()));
        user.setRole(Role.USER);
        user.setStatus(true);
        return userRepository.save(user);
    }

    @Transactional
    public boolean createAdminIfMissing(String fullName, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return false;
        }

        User admin = new User();
        admin.setFullName(fullName.trim());
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setRole(Role.ADMIN);
        admin.setStatus(true);
        userRepository.save(admin);
        return true;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
