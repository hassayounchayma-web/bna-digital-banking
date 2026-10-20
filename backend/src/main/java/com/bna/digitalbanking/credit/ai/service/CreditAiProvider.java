package com.bna.digitalbanking.credit.ai.service;

import com.bna.digitalbanking.credit.ai.dto.CreditAiFacts;

public interface CreditAiProvider {
    boolean isConfigured();
    String analyze(CreditAiFacts facts);
}
