# Flow State: Project Structure

Stack: Spring Boot, Tiger Data (TimescaleDB/Postgres), Gemini API, Presage SmartSpectra C++ SDK (Windows)

```
flow-state/
├── presage-client/                     C++ (Presage example, lightly edited)
│   ├── CMakeLists.txt
│   ├── main.cpp                        Cardio + Breathing + Face metrics → JSON lines
│   └── README.md                       Windows build steps
│
├── backend/                            Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/flowstate/
│       │   ├── FlowStateApplication.java
│       │   ├── config/
│       │   │   ├── GeminiConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   └── AsyncConfig.java
│       │   ├── controller/
│       │   │   ├── SessionController.java      start/stop focus session
│       │   │   ├── VitalsController.java       live vitals + flow score (SSE)
│       │   │   ├── CheckInController.java      "how focused were you?" 1-5
│       │   │   └── InsightController.java      Gemini reports, break suggestions
│       │   ├── model/
│       │   │   ├── FocusSession.java           task label, start, end
│       │   │   ├── VitalReading.java           hypertable: pulse, breathing, blinks
│       │   │   ├── FlowScore.java              hypertable: score per 30s window
│       │   │   ├── CheckIn.java                hypertable: self-reported focus
│       │   │   └── AppActivity.java            hypertable (stretch goal)
│       │   ├── dto/
│       │   │   ├── VitalReadingDto.java        matches the C++ JSON
│       │   │   ├── CheckInRequest.java
│       │   │   └── InsightResponse.java
│       │   ├── repository/
│       │   │   ├── SessionRepository.java
│       │   │   ├── VitalRepository.java
│       │   │   ├── FlowScoreRepository.java
│       │   │   ├── CheckInRepository.java
│       │   │   └── AppActivityRepository.java
│       │   ├── service/
│       │   │   ├── PresageReaderService.java   ProcessBuilder, reads stdout
│       │   │   ├── VitalsService.java          saves + streams readings
│       │   │   ├── FlowScoreService.java       rules: blink rate, pulse stability, talking
│       │   │   ├── CalibrationService.java     compares scores vs check-ins, adjusts weights
│       │   │   ├── StressAlertService.java     sustained pulse/breathing rise → break prompt
│       │   │   ├── GeminiService.java          break coaching, weekly focus report
│       │   │   └── ActivityTrackerService.java active window via JNA (stretch)
│       │   └── exception/
│       │       └── GlobalExceptionHandler.java
│       └── resources/
│           ├── application.yml                 ddl-auto: validate
│           └── db/migration/
│               ├── V1__core_tables.sql         focus_session
│               ├── V2__hypertables.sql         vital_reading, flow_score, check_in
│               └── V3__continuous_aggs.sql     per-minute vitals, focus by hour/weekday
│
├── frontend/                           decide later (no webcam preview, camera is exclusive)
├── .env.example                        GEMINI_API_KEY, SMARTSPECTRA_API_KEY, DB_URL
└── README.md
```

## How it works

1. User starts a focus session with a task label.
2. `PresageReaderService` launches the Presage client and streams vitals into Tiger Data.
3. `FlowScoreService` scores each 30s window from blink rate, pulse stability, and talking.
4. Every 25 minutes the user gives a 1-5 focus check-in. `CalibrationService` tunes the score to that person.
5. `StressAlertService` catches sustained stress; Gemini suggests a break based on the task and session length.
6. Tiger continuous aggregates power peak-focus insights; Gemini writes the weekly report.

## Key rules

- Hypertable entities (`VitalReading`, `FlowScore`, `CheckIn`, `AppActivity`) need composite keys: id + timestamp.
- Hibernate never creates tables. All schema lives in Flyway migrations.
- API keys live in environment variables, never committed.
- The camera is exclusive: the frontend must not open the webcam while Presage is running.

## Team split

| Person | Owns |
|---|---|
| 1 | `presage-client`, `PresageReaderService`, vitals migrations |
| 2 | `FlowScoreService`, calibration, stress alerts, Gemini |
| 3 | Frontend (live score gauge, check-in popup, insights page), seed data |