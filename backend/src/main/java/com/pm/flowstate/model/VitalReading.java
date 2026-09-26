package com.pm.flowstate.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;


import java.io.Serializable;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "vital_reading")
@IdClass(VitalReading.VitalReadingId.class)
public class VitalReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    private Instant recordedAt; // time column of the hypertable, part of the key

    @Column(nullable = false)
    private Long sessionId; // plain id, no @ManyToOne needed for the MVP

    private Double pulse;
    private Double breathing;
    private Double blinks;

    public VitalReading(Long sessionId, Instant recordedAt, Double pulse, Double breathing, Double blinks) {
        this.sessionId = sessionId;
        this.recordedAt = recordedAt;
        this.pulse = pulse;
        this.breathing = breathing;
        this.blinks = blinks;
    }

    // the composite key: field names must match the two @Id fields above
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class VitalReadingId implements Serializable {
        private Long id;
        private Instant recordedAt;
    }
}



