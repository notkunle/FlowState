// presage-client/main.cpp
//
// Wraps the SmartSpectra SDK and prints one JSON object per line to stdout:
//   {"timestamp":1732650000000,"pulseBpm":72.4,"blinkRate":14.2,"confidence":0.91}
//
// Spring Boot's PresageReaderService launches this binary, reads stdout
// line-by-line, and deserializes each line straight into VitalReadingDto.
// Keep this JSON shape and the Dto in lockstep.
//
// NOTE: the actual SmartSpectra API calls (session/config types, callback
// registration, field names on the metrics object) are isolated inside
// PresageVitalsSource below. Everything else in this file — the JSON
// cadence, stdout contract, signal handling — is what the Java side
// depends on and won't need to change once you fill that class in from
// the real headers (installed under /usr/include after
// `apt install libsmartspectra-dev libphysiologyedge-dev`) and the
// sample apps that ship alongside the SDK.

#include <atomic>
#include <chrono>
#include <csignal>
#include <cstdlib>
#include <functional>
#include <iostream>
#include <string>
#include <thread>

#include <nlohmann/json.hpp>

#include <smartspectra/smartspectra.h>   // real SDK headers go here

using json = nlohmann::json;

namespace {

std::atomic<bool> g_running{true};

void handle_sigint(int /*signal*/) {
    g_running = false;
}

long long now_ms() {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
               std::chrono::system_clock::now().time_since_epoch())
        .count();
}

struct VitalsSample {
    double pulse_bpm;
    double blink_rate;   // blinks per minute
    double confidence;   // 0.0 - 1.0 signal quality
};

// ---------------------------------------------------------------------
// PresageVitalsSource: the ONLY place that should need real SDK calls.
// ---------------------------------------------------------------------
class PresageVitalsSource {
public:
    PresageVitalsSource(std::string api_key, int camera_index)
        : api_key_(std::move(api_key)), camera_index_(camera_index) {}

    // Configure for continuous measurement, cardiac (pulse) + myofacial
    // (blink) metrics only — leave breathing/blood-pressure disabled,
    // they're not needed here and blood pressure isn't on the free tier.
    void start(std::function<void(const VitalsSample&)> on_sample) {
        on_sample_ = std::move(on_sample);

        // --- Replace this block with real SmartSpectra SDK setup ---
        // Roughly (check actual class/method names in the installed
        // headers / sample apps):
        //   smartspectra::Config config;
        //   config.set_api_key(api_key_);
        //   config.set_camera_index(camera_index_);
        //   config.enable_cardiac(true);
        //   config.enable_myofacial(true);   // blink detection
        //   config.enable_breathing(false);
        //   config.set_mode(smartspectra::Mode::Continuous);
        //
        //   session_ = smartspectra::CreateSession(config);
        //   session_->SetMetricsCallback(
        //       [this](const smartspectra::Metrics& m) {
        //           VitalsSample s{
        //               m.cardiac().pulse_rate_bpm(),
        //               m.myofacial().blink_rate_per_minute(),
        //               m.quality().confidence()
        //           };
        //           on_sample_(s);
        //       });
        //   session_->Start();
        // -------------------------------------------------------------

        running_ = true;
    }

    void stop() {
        // session_->Stop();
        running_ = false;
    }

    bool is_running() const { return running_; }

private:
    std::string api_key_;
    int camera_index_;
    bool running_ = false;
    std::function<void(const VitalsSample&)> on_sample_;
    // std::unique_ptr<smartspectra::Session> session_;
};

}  // namespace

int main(int argc, char** argv) {
    std::signal(SIGINT, handle_sigint);
    std::signal(SIGTERM, handle_sigint);

    const char* env_key = std::getenv("SMARTSPECTRA_API_KEY");
    std::string api_key = env_key ? env_key : "";
    int camera_index = 0;

    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        if (arg == "--api-key" && i + 1 < argc) {
            api_key = argv[++i];
        } else if (arg == "--camera" && i + 1 < argc) {
            camera_index = std::stoi(argv[++i]);
        }
    }

    if (api_key.empty()) {
        std::cerr << "Missing SmartSpectra API key "
                     "(set SMARTSPECTRA_API_KEY or pass --api-key)\n";
        return 1;
    }

    PresageVitalsSource source(api_key, camera_index);

    source.start([](const VitalsSample& sample) {
        json j;
        j["timestamp"] = now_ms();
        j["pulseBpm"] = sample.pulse_bpm;
        j["blinkRate"] = sample.blink_rate;
        j["confidence"] = sample.confidence;

        // One JSON object per line, flushed immediately — Java reads
        // this with BufferedReader::readLine() in a tight loop.
        std::cout << j.dump() << std::endl;
    });

    while (g_running && source.is_running()) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    source.stop();
    return 0;
}