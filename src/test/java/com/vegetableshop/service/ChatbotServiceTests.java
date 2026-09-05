package com.vegetableshop.service;

import com.vegetableshop.dto.ChatbotResponse;
import com.vegetableshop.entity.Brand;
import com.vegetableshop.entity.Category;
import com.vegetableshop.entity.Product;
import com.vegetableshop.entity.ProductUnit;
import com.vegetableshop.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTests {

    @Mock
    private ProductRepository productRepository;

    @Test
    void answersShippingFaqWithoutLoadingProducts() {
        ChatbotResponse response = service().reply("Phí giao hàng tính như thế nào?");

        assertTrue(response.message().contains("checkout"));
        assertTrue(response.products().isEmpty());
        verifyNoInteractions(productRepository);
    }

    @Test
    void findsInStockMushroomBelowRequestedPrice() {
        Product affordableMushroom = product(1L, "Nấm đùi gà", "Nấm các loại", "85000");
        Product expensiveMushroom = product(2L, "Nấm hương", "Nấm các loại", "125000");
        Product carrot = product(3L, "Cà rốt", "Rau củ", "45000");
        when(productRepository.findChatbotCandidates(eq(0), any(Pageable.class)))
            .thenReturn(List.of(affordableMushroom, expensiveMushroom, carrot));

        ChatbotResponse response = service().reply("Có nấm dưới 100.000đ không?");

        assertEquals(1, response.products().size());
        assertEquals("Nấm đùi gà", response.products().getFirst().name());
        assertEquals("/product/1", response.products().getFirst().detailUrl());
    }

    @Test
    void sortsCheapCategorySuggestionsByPrice() {
        Product carrot = product(1L, "Cà rốt", "Rau củ", "45000");
        Product pumpkin = product(2L, "Bí đỏ", "Rau củ", "25000");
        Product mushroom = product(3L, "Nấm hương", "Nấm các loại", "20000");
        when(productRepository.findChatbotCandidates(eq(0), any(Pageable.class)))
            .thenReturn(List.of(carrot, pumpkin, mushroom));

        ChatbotResponse response = service().reply("Gợi ý rau củ giá rẻ");

        assertEquals(List.of("Bí đỏ", "Cà rốt"),
            response.products().stream().map(product -> product.name()).toList());
    }

    @Test
    void returnsFriendlyMessageWhenNoProductMatches() {
        when(productRepository.findChatbotCandidates(eq(0), any(Pageable.class)))
            .thenReturn(List.of(product(1L, "Cà rốt", "Rau củ", "45000")));

        ChatbotResponse response = service().reply("Tìm sầu riêng");

        assertTrue(response.products().isEmpty());
        assertTrue(response.message().contains("chưa tìm thấy"));
    }

    private ChatbotService service() {
        return new ChatbotService(productRepository);
    }

    private Product product(Long id, String name, String categoryName, String price) {
        Category category = new Category();
        category.setId(id);
        category.setName(categoryName);
        category.setStatus(true);

        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Fresh Việt");
        brand.setStatus(true);

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(name + " tươi sạch");
        product.setPrice(new BigDecimal(price));
        product.setQuantity(20);
        product.setImage("/img/product.jpg");
        product.setCategory(category);
        product.setBrand(brand);
        product.setUnit(ProductUnit.KILOGRAM);
        product.setStatus(true);
        return product;
    }
}
