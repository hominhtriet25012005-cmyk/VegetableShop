package com.vegetableshop.event;

public record AccountActivationMailEvent(String email, String fullName, String rawToken) {
}
