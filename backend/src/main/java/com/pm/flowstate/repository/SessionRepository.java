package com.pm.flowstate.repository;


import com.pm.flowstate.model.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<FocusSession, Long> {

    Optional<FocusSession> findFirstByEndedAtIsNullOrderByStartedAtDesc(); // find the session that is still running

    boolean existsByStartedAtBefore(Instant time); // used by SeedDataRunner to seed history only once
}
