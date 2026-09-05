package com.vegetableshop.service;

import com.vegetableshop.dto.AiChatDecision;
import com.vegetableshop.entity.Product;

import java.util.List;

public interface AiChatClient {

    AiChatDecision decide(String question, List<Product> availableProducts);
}
