package com.pm.flowstate.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "focus_session")
public class FocusSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;

    @Column(nullable = false)
    private String taskLabel;

    @Column(nullable = false)
    private Instant startedAt; // instant is used because it gives a value in UTC and it can be converted to user's local time

    private Instant endedAt;

    public FocusSession(String taskLabel, Instant startedAt) {
        this.taskLabel = taskLabel;
        this.startedAt = startedAt;
    }

    public boolean isActive() {
        return endedAt == null;
    } // to ask if the seession is still running



}
