package com.vegetableshop.service;

import com.vegetableshop.dto.AiChatDecision;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotFacadeTests {

    @Mock
    private ChatbotService fallbackService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ObjectProvider<AiChatClient> provider;
    @Mock
    private AiChatClient aiClient;
    @Mock
    private ChatbotFaqService faqService;
    @Mock
    private ChatbotAnalyticsService analyticsService;

    @Test
    void managedFaqHasPriorityOverAiAndCatalogSearch() {
        ChatbotResponse official = ChatbotResponse.faq("Câu trả lời chính thức", List.of());
        when(faqService.findMatch("Phí ship?"))
            .thenReturn(Optional.of(new ChatbotFaqService.FaqMatch(12L, official)));

        ChatbotResponse result = facade().reply("Phí ship?");

        assertEquals(official, result);
        verifyNoInteractions(aiClient, productRepository, fallbackService);
        verify(analyticsService).record(
            eq("Phí ship?"), eq(official),
            eq(com.vegetableshop.entity.ChatbotResponseSource.MANAGED_FAQ),
            eq(com.vegetableshop.entity.ChatbotInteractionStatus.RESOLVED),
            any(Long.class), eq(12L)
        );
    }

    @Test
    void usesLocalAssistantWhenAiIsDisabled() {
        ChatbotResponse fallback = ChatbotResponse.message("Cơ bản", List.of());
        when(provider.getIfAvailable()).thenReturn(null);
        when(fallbackService.reply("Tìm nấm")).thenReturn(fallback);

        ChatbotResponse result = facade().reply("Tìm nấm");

        assertEquals(fallback, result);
        verifyNoInteractions(productRepository);
    }

    @Test
    void validatesAiProductIdsAgainstCurrentCatalog() {
        Product mushroom = product(1L, "Nấm đùi gà");
        Product carrot = product(2L, "Cà rốt");
        when(provider.getIfAvailable()).thenReturn(aiClient);
        when(productRepository.findChatbotCandidates(eq(0), any(Pageable.class)))
            .thenReturn(List.of(mushroom, carrot));
        when(aiClient.decide("Món nấu lẩu", List.of(mushroom, carrot)))
            .thenReturn(new AiChatDecision(
                "product_search", "Bạn có thể chọn nấm.",
                List.of(1L, 999L, 1L), List.of("Tìm rau củ", "Tìm rau củ")
            ));
        when(fallbackService.toView(mushroom)).thenCallRealMethod();

        ChatbotResponse result = facade().reply("Món nấu lẩu");

        assertEquals(ChatbotResponse.AI_MODE, result.mode());
        assertEquals(List.of(1L), result.products().stream().map(item -> item.id()).toList());
        assertEquals(List.of("Tìm rau củ"), result.suggestions());
    }

    @Test
    void fallsBackWhenOpenAiRequestFails() {
        Product mushroom = product(1L, "Nấm đùi gà");
        ChatbotResponse fallback = ChatbotResponse.message("Cơ bản", List.of());
        when(provider.getIfAvailable()).thenReturn(aiClient);
        when(productRepository.findChatbotCandidates(eq(0), any(Pageable.class)))
            .thenReturn(List.of(mushroom));
        when(aiClient.decide("Tìm nấm", List.of(mushroom)))
            .thenThrow(new IllegalStateException("timeout"));
        when(fallbackService.reply("Tìm nấm")).thenReturn(fallback);

        ChatbotResponse result = facade().reply("Tìm nấm");

        assertEquals(fallback, result);
        verify(fallbackService).reply("Tìm nấm");
    }

    private ChatbotFacade facade() {
        return new ChatbotFacade(
            fallbackService, productRepository, provider, faqService, analyticsService
        );
    }

    private Product product(Long id, String name) {
        Category category = new Category();
        category.setId(1L);
        category.setName("Rau củ");
        category.setStatus(true);

        Brand brand = new Brand();
        brand.setId(1L);
        brand.setName("Fresh Việt");
        brand.setStatus(true);

        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(name + " tươi sạch");
        product.setPrice(BigDecimal.valueOf(65000));
        product.setQuantity(20);
        product.setImage("/img/product.jpg");
        product.setCategory(category);
        product.setBrand(brand);
        product.setUnit(ProductUnit.KILOGRAM);
        product.setStatus(true);
        return product;
    }
}
