package com.vegetableshop.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(Long id) {
        super("Không tìm thấy đơn hàng có id: " + id);
    }
}
