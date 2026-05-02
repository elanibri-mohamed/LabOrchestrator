package com.mnco.security.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Normalizes platform usernames into a stable EVE-NG-safe username.
 */
@Service
public class EveNgUsernameService {

    private static final int MAX_USERNAME_LENGTH = 32;

    public String toEveNgUsername(String appUsername) {
        if (appUsername == null || appUsername.isBlank()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }

        String normalized = Normalizer.normalize(appUsername, Normalizer.Form.NFKD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_.-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[._-]+|[._-]+$", "");

        if (normalized.isBlank()) {
            normalized = "user";
        }

        if (!Character.isLetter(normalized.charAt(0))) {
            normalized = "u-" + normalized;
        }

        if (normalized.length() > MAX_USERNAME_LENGTH) {
            normalized = normalized.substring(0, MAX_USERNAME_LENGTH);
        }

        return normalized;
    }
}