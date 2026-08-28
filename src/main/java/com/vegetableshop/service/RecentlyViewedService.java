package com.vegetableshop.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecentlyViewedService {

    static final String SESSION_KEY = "recentlyViewedProductIds";
    private static final int LIMIT = 6;

    public List<Long> recordAndGetPrevious(HttpSession session, Long productId) {
        List<Long> ids = read(session);
        ids.remove(productId);
        List<Long> previous = ids.stream().limit(LIMIT).toList();
        ids.addFirst(productId);
        if (ids.size() > LIMIT) {
            ids = new ArrayList<>(ids.subList(0, LIMIT));
        }
        session.setAttribute(SESSION_KEY, ids);
        return previous;
    }

    private List<Long> read(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (!(value instanceof List<?> values)) {
            return new ArrayList<>();
        }
        List<Long> ids = new ArrayList<>();
        for (Object item : values) {
            if (item instanceof Long id) {
                ids.add(id);
            } else if (item instanceof Number number) {
                ids.add(number.longValue());
            }
        }
        return ids;
    }
}
