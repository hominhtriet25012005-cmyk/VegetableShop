package com.vegetableshop.service;

import com.vegetableshop.event.AccountActivationMailEvent;
import com.vegetableshop.event.OrderPlacedMailEvent;
import com.vegetableshop.event.OrderStatusChangedMailEvent;
import com.vegetableshop.event.PasswordResetMailEvent;

public interface MailDeliveryService {

    void sendAccountActivation(AccountActivationMailEvent event);
    void sendPasswordReset(PasswordResetMailEvent event);
    void sendOrderConfirmation(OrderPlacedMailEvent event);
    void sendOrderStatusChanged(OrderStatusChangedMailEvent event);
}
