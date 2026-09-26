package com.pm.flowstate.service;

import com.pm.flowstate.dto.VitalReadingDto;
import com.pm.flowstate.model.VitalReading;
import com.pm.flowstate.repository.SessionRepository;
import com.pm.flowstate.repository.VitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class VitalsService {
    private final VitalRepository vitalRepository;
    private final SessionRepository sessionRepository;

    // save a reading to the running session; ignored if no session is running
    public void save(VitalReadingDto dto) {
        sessionRepository.findFirstByEndedAtIsNullOrderByStartedAtDesc()
                .ifPresent(session -> vitalRepository.save(new VitalReading(
                        session.getId(), Instant.now(),
                        dto.pulse(), dto.breathing(), dto.blinks())));
    }
}