package com.pm.flowstate.repository;

import com.pm.flowstate.model.VitalReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface VitalRepository extends JpaRepository<VitalReading, VitalReading.VitalReadingId> {

    // readings for one session within a time window (baseline = first 3 min, summary = last 30s)
    List<VitalReading> findBySessionIdAndRecordedAtBetween(Long sessionId, Instant from, Instant to);
}
