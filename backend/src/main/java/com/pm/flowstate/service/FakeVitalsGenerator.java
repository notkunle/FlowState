package com.pm.flowstate.service;

import com.pm.flowstate.dto.VitalReadingDto;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

// stands in for presage-client when there's no camera (flowstate.vitals.source=fake)
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "flowstate.vitals.source", havingValue = "fake")
public class FakeVitalsGenerator {
    private final VitalsService vitalsService;

    @Scheduled(fixedRate = 1000)
    public void emit() {
        vitalsService.save(new VitalReadingDto(
                around(72, 5),   // pulse
                around(15, 2),   // breathing
                around(17, 6))); // blinks
    }

    private double around(double value, double spread) {
        double v = value + ThreadLocalRandom.current().nextDouble(-spread, spread);
        return Math.round(v * 10) / 10.0; // one decimal
    }
}