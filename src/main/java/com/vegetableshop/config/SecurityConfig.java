package com.vegetableshop.config;

import com.vegetableshop.service.GoogleOidcUserService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Authentication and authorization rules for customer and Admin pages.
 */
@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        ObjectProvider<ClientRegistrationRepository> clientRegistrations,
        ObjectProvider<GoogleOidcUserService> googleOidcUserService
    ) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, "/product/*/reviews")
                .hasAnyRole("USER", "ADMIN")
                .requestMatchers(
                    "/", "/shop", "/product/**", "/news", "/contact", "/testimonial",
                    "/login", "/register", "/forgot-password", "/reset-password",
                    "/activate-account", "/resend-activation",
                    "/api/chatbot/**",
                    "/oauth2/**", "/login/oauth2/**",
                    "/access-denied", "/error", "/error/**",
                    "/css/**", "/js/**", "/img/**", "/lib/**"
                ).permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(
                    "/account/**", "/cart/**", "/api/cart/**", "/checkout",
                    "/my-orders/**", "/orders/**", "/wishlist/**", "/api/wishlist/**"
                )
                .hasAnyRole("USER", "ADMIN")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedPage("/access-denied")
            );

        ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
        GoogleOidcUserService oidcUserService = googleOidcUserService.getIfAvailable();
        if (registrations != null && oidcUserService != null) {
            http.oauth2Login(oauth -> oauth
                .loginPage("/login")
                .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?oauthError")
            );
        }

        return http.build();
    }
}
