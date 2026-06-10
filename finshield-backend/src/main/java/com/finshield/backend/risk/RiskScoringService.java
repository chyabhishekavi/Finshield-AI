package com.finshield.backend.risk;

import com.finshield.backend.transaction.event.TransactionEvent;

public interface RiskScoringService {

    RiskScoringResult score(TransactionEvent event);
}
