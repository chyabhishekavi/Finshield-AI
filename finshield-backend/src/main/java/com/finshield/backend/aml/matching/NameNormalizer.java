package com.finshield.backend.aml.matching;

import java.text.Normalizer;
import java.util.Locale;

public final class NameNormalizer {

    private NameNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        return decomposed
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N} ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
