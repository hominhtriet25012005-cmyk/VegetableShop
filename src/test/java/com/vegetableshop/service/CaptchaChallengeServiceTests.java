package com.vegetableshop.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaptchaChallengeServiceTests {

    @Test
    void issuedChallengeCanBeSolvedOnlyOnce() {
        CaptchaChallengeService service = new CaptchaChallengeService();
        MockHttpSession session = new MockHttpSession();

        service.issue(session, "register");
        var challenge = (CaptchaChallengeService.CaptchaChallenge) session.getAttribute(
            CaptchaChallengeService.SESSION_PREFIX + "register");

        assertTrue(service.verify(session, "register", " " + challenge.answer() + " "));
        assertFalse(service.verify(session, "register", challenge.answer()));
    }

    @Test
    void expiredOrIncorrectChallengeIsRejectedAndRemoved() {
        CaptchaChallengeService service = new CaptchaChallengeService();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
            CaptchaChallengeService.SESSION_PREFIX + "forgot-password",
            new CaptchaChallengeService.CaptchaChallenge("2 + 2 = ?", "4", Instant.now().minusSeconds(1))
        );

        assertFalse(service.verify(session, "forgot-password", "4"));
        service.issue(session, "forgot-password");
        assertFalse(service.verify(session, "forgot-password", "999"));
        assertFalse(service.verify(session, "forgot-password", "999"));
    }
}
