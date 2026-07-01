package com.finshield.backend.aml.pattern;

import com.finshield.backend.transaction.domain.Transaction;

public interface AmlDetectionService {

    AmlDetectionResult detect(Transaction transaction);
}
