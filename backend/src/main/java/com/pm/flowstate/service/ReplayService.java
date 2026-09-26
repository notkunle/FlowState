package com.pm.flowstate.service;

import com.pm.flowstate.dto.VitalReadingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// replays a recorded session one line per second, looping (flowstate.vitals.source=replay)
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "flowstate.vitals.source", havingValue = "replay")
public class ReplayService {
    private final VitalsService vitalsService;
    private final JsonMapper jsonMapper;

    @Value("${flowstate.replay.file}")
    private Resource file;

    private List<VitalReadingDto> readings;
    private final AtomicInteger next = new AtomicInteger();

    @Scheduled(fixedRate = 1000)
    public void emit() throws IOException {
        if (readings == null) {
            readings = file.getContentAsString(StandardCharsets.UTF_8).lines()
                    .filter(line -> line.startsWith("{"))
                    .map(line -> jsonMapper.readValue(line, VitalReadingDto.class))
                    .toList();
            log.info("Replaying {} readings from {}", readings.size(), file.getFilename());
        }
        vitalsService.save(readings.get(next.getAndIncrement() % readings.size()));
    }
}
