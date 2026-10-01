package com.ecommerce.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginRateLimiterTest {

    private static final String IP = "10.0.0.1";

    private final MutableClock clock = new MutableClock(Instant.parse("2026-10-01T10:00:00Z"));
    private final LoginRateLimiter limiter =
        new LoginRateLimiter(new LoginRateLimitProperties(5, Duration.ofMinutes(15)), clock);

    @Test
    void blocksAfterFiveFailuresWithinTheWindowUntilTheOldestExpires() {
        failTimes("ana@example.com", 4);
        assertThatCode(() -> limiter.checkAllowed("ana@example.com", IP)).doesNotThrowAnyException();

        limiter.recordFailure("ana@example.com", IP);

        assertThatThrownBy(() -> limiter.checkAllowed("ANA@example.com", IP))
            .isInstanceOf(TooManyLoginAttemptsException.class)
            .satisfies(ex -> assertThat(((TooManyLoginAttemptsException) ex).getRetryAfter())
                .isEqualTo(Duration.ofMinutes(15).minusSeconds(4)));
    }

    @Test
    void otherEmailsAndIpsAreNotAffected() {
        failTimes("ana@example.com", 5);

        assertThatCode(() -> limiter.checkAllowed("luis@example.com", IP)).doesNotThrowAnyException();
        assertThatCode(() -> limiter.checkAllowed("ana@example.com", "10.0.0.2")).doesNotThrowAnyException();
    }

    @Test
    void unblocksWhenTheOldestFailureLeavesTheWindow() {
        failTimes("ana@example.com", 5);

        clock.advance(Duration.ofMinutes(15).plusSeconds(1));

        assertThatCode(() -> limiter.checkAllowed("ana@example.com", IP)).doesNotThrowAnyException();
    }

    @Test
    void successfulLoginResetsTheCounter() {
        failTimes("ana@example.com", 4);
        limiter.recordSuccess("ana@example.com", IP);
        failTimes("ana@example.com", 4);

        assertThatCode(() -> limiter.checkAllowed("ana@example.com", IP)).doesNotThrowAnyException();
    }

    private void failTimes(String email, int times) {
        for (int i = 0; i < times; i++) {
            limiter.recordFailure(email, IP);
            clock.advance(Duration.ofSeconds(1));
        }
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
