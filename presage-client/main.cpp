// presage-client/main.cpp
//
// Reads vitals from the webcam via the SmartSpectra C++ SDK and prints one
// JSON object per reading to STDOUT (one per line). PresageReaderService on
// the Spring Boot side reads this process's stdout line-by-line and parses
// each line into a VitalReadingDto.
//
// IMPORTANT: stdout is reserved for JSON-lines output only. All human-
// readable status/error logging goes to stderr, so it never corrupts the
// stream the backend is parsing.
//
// Field names below are taken from Presage's own cpp/docs/metrics.md
// (breathing().rate().value()/.timestamp()/.stable(),
// cardio().pulse_rate().value()/.timestamp(), face().blinking().detected()).
// One thing that doc makes explicit: there is no SDK-provided "blink rate" —
// blinking() is a per-frame detected()/not-detected() event, not a rate.
// This file emits the raw detection events; turning that into a rate
// (blinks per minute) belongs in VitalsSummaryService on the backend, which
// already needs to do 30s-window math for the baseline comparison.
//
// Two fields are used by analogy with documented patterns rather than
// confirmed verbatim in the docs — .stable() on pulse_rate (confirmed only
// for breathing rate in the docs, but described generically as a property
// of "Measurement types") and a timestamp on the blinking DetectionStatus
// (not shown in the docs at all). Both are marked below; verify against
// smartspectra/messages/metrics.h if either fails to compile.

#include <smartspectra/messages/metric_types.pb.h>
#include <smartspectra/messages/metrics.h>
#include <smartspectra/smartspectra.h>
#include <smartspectra/smartspectra_config.h>

#include <chrono>
#include <csignal>
#include <cstdlib>
#include <iostream>
#include <sstream>
#include <string>
#include <thread>

namespace spectra = presage::smartspectra;

namespace {

volatile std::sig_atomic_t g_stop_requested = 0;

void HandleSignal(int) {
    g_stop_requested = 1;
}

std::string ResolveApiKey(int argc, char** argv) {
    if (argc > 1) {
        return argv[1];
    }
    if (const char* key = std::getenv("SMARTSPECTRA_API_KEY")) {
        return key;
    }
    return {};
}

int64_t NowEpochMs() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::system_clock::now().time_since_epoch())
        .count();
}

}  // namespace

int main(int argc, char** argv) {
    std::signal(SIGINT, HandleSignal);
    std::signal(SIGTERM, HandleSignal);

    const std::string api_key = ResolveApiKey(argc, argv);
    if (api_key.empty()) {
        std::cerr << "Usage: presage-client.exe YOUR_API_KEY\n"
                  << "or set SMARTSPECTRA_API_KEY=YOUR_API_KEY\n";
        return 1;
    }

    spectra::SmartSpectraConfig config;
    config.api_key = api_key;
    config.requested_metrics = spectra::SmartSpectraConfig::BreathingMetrics();
    config.AddMetrics(spectra::SmartSpectraConfig::CardioMetrics());
    config.AddMetrics(spectra::SmartSpectraConfig::FaceMetrics());
    // Explicitly requested: cpp/docs/metrics.md's advanced example lists
    // BLINKING separately from the FaceMetrics() bundle, so don't assume
    // the bundle already includes it.
    config.AddMetrics({spectra::MetricType::BLINKING});

    spectra::SmartSpectra sdk(config);

    sdk.SetOnMetrics([](const spectra::Metrics& metrics, int64_t timestamp_us) {
        std::ostringstream json;
        json << "{"
             << "\"recordedAt\":" << NowEpochMs() << ","
             << "\"sdkTimestampUs\":" << timestamp_us;

        // Peak/event-driven: rate_size() can legitimately be 0 between
        // valid updates (per metrics.md) — that's not an error, just no
        // new sample this callback.
        if (metrics.has_breathing() && metrics.breathing().rate_size() > 0) {
            const auto& rate = metrics.breathing().rate(metrics.breathing().rate_size() - 1);
            json << ",\"breathingRate\":" << rate.value()
                 << ",\"breathingRateTimestampUs\":" << rate.timestamp()
                 << ",\"breathingRateStable\":" << (rate.stable() ? "true" : "false");
        }

        if (metrics.has_cardio() && metrics.cardio().pulse_rate_size() > 0) {
            const auto& pulse = metrics.cardio().pulse_rate(metrics.cardio().pulse_rate_size() - 1);
            json << ",\"pulseRate\":" << pulse.value()
                 << ",\"pulseRateTimestampUs\":" << pulse.timestamp();
            // stable() is documented generically for "Measurement types" but
            // only demonstrated on breathing().rate() in the docs — if this
            // line fails to compile, drop it and check metrics.h directly.
            json << ",\"pulseRateStable\":" << (pulse.stable() ? "true" : "false");
        }

        // Frame-driven, not a rate: raw detection events. Compute an actual
        // blink rate (events per minute) downstream in VitalsSummaryService.
        if (metrics.has_face() && metrics.face().blinking_size() > 0) {
            const auto& blink = metrics.face().blinking(metrics.face().blinking_size() - 1);
            json << ",\"blinkDetected\":" << (blink.detected() ? "true" : "false");
        }

        json << "}";

        std::cout << json.str() << std::endl;  // flush every line
    });

    sdk.SetOnValidationStatusChanged(
        [](const spectra::ValidationStatus& status, int64_t) {
            std::cerr << "Validation [" << status.code << "]: " << status.hint << "\n";
        });

    sdk.SetOnError([](const spectra::SmartSpectraError& error) {
        std::cerr << "Error [" << static_cast<int>(error.code) << "]: " << error.message << "\n";
    });

    const auto source_error = sdk.UseCamera().SetResolution(1280, 720).SetFps(30).Build();
    if (!source_error.ok()) {
        std::cerr << "Failed to create camera source: " << source_error.message << "\n";
        return 1;
    }

    if (const auto err = sdk.Start(); !err.ok()) {
        std::cerr << "Failed to start: " << err.message << "\n";
        return 1;
    }

    std::cerr << "presage-client running. Press Ctrl+C to stop.\n";
    while (!g_stop_requested) {
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
    }

    if (const auto err = sdk.Stop(); !err.ok()) {
        std::cerr << "Stop failed: " << err.message << "\n";
    }
    return 0;
}