# Flow State: MVP Project Structure

**Stack:** Spring Boot, Tiger Data (Postgres + TimescaleDB), Gemini API, Presage SmartSpectra C++ SDK (Windows)

**One-line pitch:** Flow State learns what focus looks like in your body, then protects it.

## Project Structure

```
flow-state/
├── presage-client/                    C++ (Presage example, lightly edited)
│   ├── CMakeLists.txt
│   ├── main.cpp                       prints vitals as JSON lines
│   └── README.md                      Windows build steps
│
├── backend/                           Spring Boot
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/flowstate/
│       │   ├── FlowStateApplication.java
│       │   ├── config/
│       │   │   ├── GeminiConfig.java
│       │   │   ├── CorsConfig.java
│       │   │   ├── AsyncConfig.java
│       │   │   └── SeedDataRunner.java       fake week of history for demo
│       │   ├── controller/
│       │   │   ├── SessionController.java    start/stop session
│       │   │   ├── VitalsController.java     live state stream (SSE)
│       │   │   └── InsightController.java    focus-by-hour chart data
│       │   ├── model/
│       │   │   ├── FocusSession.java         task label, start, end
│       │   │   ├── VitalReading.java         hypertable: pulse, breathing, blinks
│       │   │   └── StateDecision.java        hypertable: Gemini state + reason
│       │   ├── dto/
│       │   │   ├── VitalReadingDto.java      matches the C++ JSON
│       │   │   ├── VitalsSummary.java        30s averages vs baseline
│       │   │   └── GeminiDecision.java       state, reason, suggestion
│       │   ├── repository/
│       │   │   ├── SessionRepository.java
│       │   │   ├── VitalRepository.java
│       │   │   └── DecisionRepository.java
│       │   ├── service/
│       │   │   ├── PresageReaderService.java   runs presage-client, reads stdout
│       │   │   ├── FakeVitalsGenerator.java    same JSON, no camera needed
│       │   │   ├── ReplayService.java          replays a recorded session
│       │   │   ├── VitalsService.java          saves + streams readings
│       │   │   ├── VitalsSummaryService.java   baseline + 30s summary
│       │   │   └── GeminiService.java          summary → state decision
│       │   └── exception/
│       │       └── GlobalExceptionHandler.java
│       └── resources/
│           ├── application.yml                 ddl-auto: validate
│           ├── recordings/
│           │   └── demo-session.jsonl          backup for the live demo
│           └── db/migration/
│               ├── V1__focus_session.sql
│               ├── V2__hypertables.sql         vital_reading, state_decision
│               └── V3__hourly_focus_agg.sql    continuous aggregate
│
├── frontend/                          decide later (no webcam preview)
├── .env.example                       GEMINI_API_KEY, SMARTSPECTRA_API_KEY, DB_URL
└── README.md
```

## How It Works

1. User types a task and starts a session.
2. Presage reads pulse, breathing, and blinks from the webcam and prints JSON lines.
3. Spring Boot stores every reading in Tiger Data.
4. Every 30 seconds, the backend compares the latest readings to the user's first few minutes (their baseline).
5. Every 1 to 2 minutes, Gemini receives that summary plus the task and decides: focus, stress, or neutral, with a reason and a suggestion.
6. The frontend shows the current state, Gemini's reason, and a break card when stress is detected.
7. The insights page shows focus by hour from a Tiger continuous aggregate over seeded history.

## MVP Scope

**In:**
- Presage vitals into Tiger Data (fake generator as backup)
- Baseline and 30-second summaries
- Gemini state decision as structured JSON
- Live state, reason, and break card in the frontend
- One insights chart
- Replay mode for demo safety

**Cut unless time allows (in this order):**
1. Slack DND and status
2. Focus check-ins
3. App usage tracking
4. Gemini function calling
5. Natural-language "ask your data"

## Build Order

| Step | Owner | Difficulty | Time |
|---|---|---|---|
| Presage running + fake generator | P1 | 3/5 | 2h |
| Save vitals to Tiger | P1 | 2/5 | 1h |
| 30-second summary | P2 | 1/5 | 1h |
| Gemini state call | P2 | 2/5 | 1.5h |
| Frontend: state, reason, break card | P3 | 2/5 | 3h |
| Seed data + insights chart | P3 | 2/5 | 2h |
| Replay mode | P1 | 1/5 | 1h |

## Key Rules

- Hypertable entities (`VitalReading`, `StateDecision`) need composite keys: id + timestamp.
- Hibernate never creates tables. All schema lives in Flyway migrations.
- API keys live in environment variables, never committed.
- The camera is exclusive: the frontend must not open the webcam while Presage runs.
- Pitch the score as relative to the user's own baseline, not a medical-grade flow detector.