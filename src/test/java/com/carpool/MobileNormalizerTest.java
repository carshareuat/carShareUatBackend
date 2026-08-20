package com.carpool;

import com.carpool.exception.AppException;
import com.carpool.validation.MobileNormalizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MobileNormalizerTest {

    private final MobileNormalizer normalizer = new MobileNormalizer();

    @Test
    void shouldNormalizeMobile() {
        String normalized = normalizer.normalize(" 919812345678 ");
        Assertions.assertEquals("+919812345678", normalized);
    }

    @Test
    void shouldRejectInvalidMobile() {
        Assertions.assertThrows(AppException.class, () -> normalizer.normalize("abc"));
    }
}
