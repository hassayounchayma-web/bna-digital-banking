package com.bna.digitalbanking.simulation;

import java.math.BigDecimal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/parameters")
public class CreditParameterController {
    private final CreditParameterService parameterService;

    public CreditParameterController(CreditParameterService parameterService) {
        this.parameterService = parameterService;
    }

    @GetMapping
    public SimulationParametersResponse getParameters() {
        return parameterService.getParameters();
    }

    @PutMapping("/tmm")
    public TmmResponse updateTmm(@Valid @RequestBody TmmRequest request) {
        BigDecimal value = parameterService.updateTmm(request.valuePercent());
        return new TmmResponse(value);
    }

    @PutMapping("/margins")
    public CreditMarginResponse saveMargin(@Valid @RequestBody CreditMarginRequest request) {
        return parameterService.saveMargin(request);
    }

    public record TmmResponse(BigDecimal valuePercent) {
    }
}
