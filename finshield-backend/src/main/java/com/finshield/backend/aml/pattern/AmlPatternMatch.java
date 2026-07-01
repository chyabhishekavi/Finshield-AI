package com.finshield.backend.aml.pattern;

import java.math.BigDecimal;

public record AmlPatternMatch(
        AmlPattern pattern,
        BigDecimal scoreImpact,
        String explanation
) {
}
