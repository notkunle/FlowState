# presage-client

Reads webcam vitals via the SmartSpectra C++ SDK and prints one JSON object
per reading to stdout. FlowState's backend (`PresageReaderService`) runs this
executable as a subprocess and parses its stdout line-by-line.

This builds **natively on Windows** — no WSL, no apt PPA, no Linux toolchain.
(An earlier attempt went down the WSL/apt route based on the incorrect
assumption that the SDK has no Windows build; it does, via a prebuilt ZIP.)

## Prerequisites

- Visual Studio Build Tools 2022 or later (or a full Visual Studio 2022+
  install, Community edition is fine), with the **Desktop development with
  C++** workload, including **C++ CMake tools for Windows**. This gives you
  MSVC, the Windows SDK, CMake, and the developer command prompt.
- A SmartSpectra API key from https://physiology.presagetech.com/auth/login

## Setup

1. Download `smartspectra-sdk-<version>-windows-x64.zip` from
   https://github.com/Presage-Security/SmartSpectra/releases and extract it
   somewhere permanent, e.g. `C:\SmartSpectra`. Keep the extracted
   `include/`, `lib/`, `bin/`, `share/` layout intact — the SDK resolves its
   bundled models/resources relative to `smartspectra.dll` at
   `bin/../share/smartspectra`, so don't move DLLs out on their own.

2. Open the **x64 Developer Command Prompt** (Start menu → search for it;
   a full Visual Studio install names it "x64 Native Tools Command Prompt
   for VS 2022"). Building outside this prompt is the most common cause of
   "cannot open source file" style errors — it's not a missing include path,
   it's the wrong compiler environment.

3. From this folder (`presage-client/`):

   ```bat
   set "SMARTSPECTRA_SDK_PATH=C:\SmartSpectra"
   cmake -S . -B build -G "NMake Makefiles" -DCMAKE_BUILD_TYPE=Release
   cmake --build build
   ```

4. Run it — the SDK's runtime DLLs (`smartspectra.dll`, `opencv_world*.dll`,
   `vulkan-1.dll`) need to be on `PATH`:

   ```bat
   set "PATH=%SMARTSPECTRA_SDK_PATH%\bin;%PATH%"
   set "SMARTSPECTRA_API_KEY=YOUR_API_KEY"
   .\build\presage-client.exe
   ```

   You should see `presage-client running. Press Ctrl+C to stop.` on
   stderr, then one JSON line per reading on stdout as soon as the camera
   picks you up.

## Known gap: exact metric field names

`main.cpp` currently embeds each metric's `ShortDebugString()` as a string
inside the JSON rather than pulling out numeric fields like pulse rate,
breathing rate, or blink rate directly — the SDK's public docs don't expose
the exact protobuf accessor names. Before wiring this into
`VitalReadingDto` on the backend:

1. Open `include/smartspectra/messages/metrics.h` (and the `.proto` it's
   generated from) inside the extracted SDK.
2. Find the real accessors on `Metrics::Cardio`, `Metrics::Breathing`, and
   `Metrics::Face` for pulse rate, breathing rate, and blink rate/count.
3. Replace the `TODO` blocks in `main.cpp`'s `SetOnMetrics` callback with
   those real getters, emitting numeric JSON fields instead of a debug
   string.

## Deploying alongside the Spring Boot backend

`PresageReaderService` invokes this executable directly (no WSL bridging
needed, since everything now runs natively on the same Windows host). Ship
`presage-client.exe` together with the SDK's `bin/` and `share/` directories
as a unit — the backend's `flowstate.presage.executable` path needs the
`bin/` directory (or wherever the exe sits alongside the DLLs) on `PATH` in
the environment that launches it.