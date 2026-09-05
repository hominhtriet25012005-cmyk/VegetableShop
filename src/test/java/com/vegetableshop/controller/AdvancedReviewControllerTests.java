package com.vegetableshop.controller;

import com.vegetableshop.dto.ReviewRequest;
import com.vegetableshop.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AdvancedReviewControllerTests {
    @Test void multipartBindsPurchaseAndPhotosAndRejectsMissingPurchase() throws Exception {
        var service = mock(ReviewService.class);
        var controller = new ProductController(null,null,service,null,null,null,null);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        var principal = UsernamePasswordAuthenticationToken.authenticated("customer@example.com", "", List.of());
        mvc.perform(multipart("/product/1/reviews").file(new MockMultipartFile("images", "photo.png", "image/png", new byte[]{1,2}))
            .param("orderDetailId", "10").param("rating", "5").param("comment", "Tươi ngon").principal(principal))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/product/1#reviews"))
            .andExpect(flash().attributeExists("successMessage"));
        verify(service).save(eq("customer@example.com"), eq(1L), argThat((ReviewRequest r) ->
            r.getOrderDetailId() == 10L && r.getImages().size() == 1 && r.getRating() == 5));
        reset(service);
        mvc.perform(multipart("/product/1/reviews").param("rating", "5").principal(principal))
            .andExpect(flash().attributeExists("errorMessage"));
        verifyNoInteractions(service);
    }
}
