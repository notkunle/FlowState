package com.pm.flowstate.service;

import com.pm.flowstate.dto.VitalReadingDto;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

// runs presage-client (webcam) and saves every JSON line it prints (flowstate.vitals.source=presage)
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "flowstate.vitals.source", havingValue = "presage")
public class PresageReaderService {
    private final VitalsService vitalsService;
    private final JsonMapper jsonMapper;

    @Value("${flowstate.presage.executable}")
    private String executable;

    @Value("${flowstate.presage.api-key}")
    private String apiKey;

    private Process process;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Thread reader = new Thread(this::run, "presage-reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void run() {
        try {
            ProcessBuilder builder = new ProcessBuilder(executable);
            builder.environment().put("SMARTSPECTRA_API_KEY", apiKey);
            builder.redirectError(ProcessBuilder.Redirect.INHERIT); // presage logs show up in our console
            process = builder.start();
            log.info("Started presage-client: {}", executable);

            try (BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = out.readLine()) != null) {
                    if (!line.startsWith("{")) {
                        continue; // not a reading
                    }
                    try {
                        vitalsService.save(jsonMapper.readValue(line, VitalReadingDto.class));
                    } catch (Exception e) {
                        log.warn("Skipping bad presage line: {}", line);
                    }
                }
            }
            log.warn("presage-client exited with code {}", process.waitFor());
        } catch (IOException e) {
            log.error("Could not start presage-client at {}. Use VITALS_SOURCE=fake or replay.", executable, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @PreDestroy
    public void stop() {
        if (process != null) {
            process.destroy(); // frees the camera
        }
    }
}
