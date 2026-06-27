package com.finshield.backend.aml.matching;

import com.finshield.backend.aml.domain.AmlMatchType;

import java.math.BigDecimal;

public record NameMatch(boolean matched, AmlMatchType matchType, BigDecimal score) {
}
