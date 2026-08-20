package com.carpool.validation;

import com.carpool.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class MobileNormalizer {

    public String normalize(String input) {
        if (input == null || input.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Mobile is required");
        }
        String m = input.trim().replace(" ", "");
        if (!m.startsWith("+")) {
            m = "+" + m;
        }
        if (!m.matches("^\\+[0-9]{8,15}$")) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "SEMANTIC_ERROR", "Invalid mobile format");
        }
        return m;
    }
}
