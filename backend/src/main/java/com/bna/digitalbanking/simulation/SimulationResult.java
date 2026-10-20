package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;

public record SimulationResult(
    BigDecimal totalRevenuNetMoyen,
    BigDecimal tauxEndettement,
    BigDecimal gf,
    BigDecimal plafondEndettementMensuel,
    BigDecimal availableMensualite,
    BigDecimal reliquatCapaciteEffectif,
    BigDecimal montantMaxCredit,
    BigDecimal mensualite,
    Integer duree,
    BigDecimal tauxAnnuel,
    BigDecimal totalEndettementMensuel,
    BigDecimal totalRetenues,
    BigDecimal tauxEndettementCalcule,
    BigDecimal reliquatCreditApresRachat,
    String statut,
    String error
) {
    static SimulationResult error(String message) {
        return new SimulationResult(
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, "HORS_LIMITES", message);
    }

    static SimulationResult capacityInsufficient(SimulationResult base) {
        return new SimulationResult(
            base.totalRevenuNetMoyen(), base.tauxEndettement(), base.gf(),
            base.plafondEndettementMensuel(), base.availableMensualite(),
            base.reliquatCapaciteEffectif(), BigDecimal.ZERO, base.mensualite(),
            base.duree(), base.tauxAnnuel(), base.totalEndettementMensuel(),
            base.totalRetenues(), base.tauxEndettementCalcule(),
            base.reliquatCreditApresRachat(), "CAPACITE_INSUFFISANTE",
            "Capacité Insuffisante");
    }
}
