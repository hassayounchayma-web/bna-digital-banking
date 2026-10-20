package com.bna.digitalbanking.simulation;

import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/simulations")
public class SimulationController {
    private final SimulationService simulationService;
    private final CreditParameterService parameterService;

    public SimulationController(
            SimulationService simulationService,
            CreditParameterService parameterService) {
        this.simulationService = simulationService;
        this.parameterService = parameterService;
    }

    @PostMapping("/sans-rachat")
    public SimulationResult simulateSansRachat(@Valid @RequestBody SansRachatRequest request) {
        return simulationService.simulateSansRachat(request);
    }

    @PostMapping("/rachat-interne")
    public SimulationResult simulateRachatInterne(@Valid @RequestBody RachatInterneRequest request) {
        return simulationService.simulateRachatInterne(request);
    }

    @PostMapping("/rachat-externe")
    public SimulationResult simulateRachatExterne(@Valid @RequestBody RachatExterneRequest request) {
        return simulationService.simulateRachatExterne(request);
    }

    @GetMapping("/catalogue")
    public Map<String, CreditRateConfig> catalogue(@RequestParam CreditScenario scenario) {
        return parameterService.getCatalogue(scenario);
    }
}
