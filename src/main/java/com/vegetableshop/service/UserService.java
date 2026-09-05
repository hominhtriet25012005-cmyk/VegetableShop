package com.vegetableshop.service;

import com.vegetableshop.dto.ProfileUpdateRequest;
import com.vegetableshop.dto.RegisterRequest;
import com.vegetableshop.entity.AuthProvider;
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
            .filter(User::isEmailVerified)
            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy tài khoản đang hoạt động"));
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return email != null && userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    @Transactional
    public User register(RegisterRequest request) {
        return register(request, true);
    }

    @Transactional
    public User register(RegisterRequest request, boolean emailVerified) {
        String email = normalizeEmail(request.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("Email này đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setAddress(normalizeOptional(request.getAddress()));
        user.setRole(Role.USER);
        user.setStatus(true);
        user.setEmailVerified(emailVerified);
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
        admin.setAuthProvider(AuthProvider.LOCAL);
        admin.setRole(Role.ADMIN);
        admin.setStatus(true);
        admin.setEmailVerified(true);
        userRepository.save(admin);
        return true;
    }

    @Transactional(readOnly = true)
    public ProfileUpdateRequest createProfileRequest(String email) {
        User user = findByEmail(email);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(user.getFullName());
        request.setPhone(user.getPhone());
        request.setAddress(user.getAddress());
        return request;
    }

    @Transactional
    public User updateProfile(String email, ProfileUpdateRequest request) {
        User user = findByEmail(email);
        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizeOptional(request.getPhone()));
        user.setAddress(normalizeOptional(request.getAddress()));
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findByEmail(email);
        if (user.getPassword() != null && !passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User upsertGoogleUser(String subject, String email, String fullName) {
        String normalizedSubject = requireText(subject, "Google không trả về mã định danh người dùng");
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByOauthSubject(normalizedSubject)
            .orElseGet(() -> userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null));

        if (user == null) {
            user = new User();
            user.setFullName(normalizeGoogleName(fullName, normalizedEmail));
            user.setEmail(normalizedEmail);
            user.setPassword(null);
            user.setAuthProvider(AuthProvider.GOOGLE);
            user.setOauthSubject(normalizedSubject);
            user.setRole(Role.USER);
            user.setStatus(true);
            user.setEmailVerified(true);
            return userRepository.save(user);
        }

        if (!user.isStatus()) {
            throw new IllegalStateException("Tài khoản đã bị khóa");
        }
        if (user.getOauthSubject() != null && !user.getOauthSubject().equals(normalizedSubject)) {
            throw new IllegalStateException("Email đã được liên kết với một tài khoản Google khác");
        }

        user.setOauthSubject(normalizedSubject);
        user.setEmailVerified(true);
        if (user.getAuthProvider() == null) {
            user.setAuthProvider(AuthProvider.LOCAL);
        }
        return userRepository.save(user);
    }

    @Transactional
    public void setPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeGoogleName(String fullName, String email) {
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim().substring(0, Math.min(fullName.trim().length(), 100));
        }
        String localPart = email.substring(0, email.indexOf('@'));
        return localPart.substring(0, Math.min(localPart.length(), 100));
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
