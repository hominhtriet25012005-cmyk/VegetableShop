package com.vegetableshop.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecentlyViewedServiceTests {

    @Test
    void currentProductMovesToFrontAndPreviousProductsAreReturned() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(RecentlyViewedService.SESSION_KEY, new java.util.ArrayList<>(List.of(2L, 1L)));

        List<Long> previous = new RecentlyViewedService().recordAndGetPrevious(session, 1L);

        assertEquals(List.of(2L), previous);
        assertEquals(List.of(1L, 2L), session.getAttribute(RecentlyViewedService.SESSION_KEY));
    }
}
