package com.pm.flowstate.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "state_decision")
@IdClass(StateDecision.StateDecisionId.class)
public class StateDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    private Instant decidedAt; // time column of the hypertable, part of the key

    @Column(nullable = false)
    private Long sessionId;

    @Column(nullable = false)
    private String state; // focus / stress / neutral

    private String reason;
    private String suggestion;

    public StateDecision(Long sessionId, Instant decidedAt, String state, String reason, String suggestion) {
        this.sessionId = sessionId;
        this.decidedAt = decidedAt;
        this.state = state;
        this.reason = reason;
        this.suggestion = suggestion;
    }

    // the composite key: field names must match the two @Id fields above
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class StateDecisionId implements Serializable {
        private Long id;
        private Instant decidedAt;
    }
}
