package com.vegetableshop.config;

import com.vegetableshop.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mysql")
public class AdminAccountInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserService userService;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(
        UserService userService,
        @Value("${app.admin.name:Quản trị viên}") String adminName,
        @Value("${app.admin.email:}") String adminEmail,
        @Value("${app.admin.password:}") String adminPassword
    ) {
        this.userService = userService;
        this.adminName = adminName;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() && adminPassword.isBlank()) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.length() < 8 || adminPassword.length() > 72) {
            logger.warn("Không tạo Admin: APP_ADMIN_EMAIL và APP_ADMIN_PASSWORD (8-72 ký tự) chưa hợp lệ");
            return;
        }

        if (userService.createAdminIfMissing(adminName, adminEmail, adminPassword)) {
            logger.info("Đã tạo tài khoản Admin ban đầu cho email {}", adminEmail.trim());
        } else {
            logger.info("Không tạo Admin vì email {} đã tồn tại", adminEmail.trim());
        }
    }
}
