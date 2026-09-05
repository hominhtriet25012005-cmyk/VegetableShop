package com.vegetableshop.event;

public record PasswordResetMailEvent(String email, String fullName, String rawToken) {
}
