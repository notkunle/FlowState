cmake_minimum_required(VERSION 3.19)
project(presage-client LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Installed via the Presage PPA:
#   sudo apt install libphysiologyedge-dev libsmartspectra-dev
#
# The exact CMake package/target names below are placeholders — after
# installing, check /usr/lib/cmake/ or /usr/lib/x86_64-linux-gnu/cmake/ for
# the *Config.cmake files the packages actually ship, and adjust
# find_package()/target_link_libraries() to match.
list(APPEND CMAKE_PREFIX_PATH "$ENV{SMARTSPECTRA_SDK_PATH}")
find_package(SmartSpectra CONFIG REQUIRED)
target_link_libraries(your_target SmartSpectra::SDK)

add_executable(presage-client main.cpp)

target_include_directories(presage-client PRIVATE /usr/include)

target_link_libraries(presage-client
    PRIVATE
        nlohmann_json::nlohmann_json
        pthread
        # smartspectra / physiologyedge shared libs — replace with the
        # correct target/lib name once confirmed, e.g.:
        # SmartSpectra::SmartSpectra
        # or plain: -lsmartspectra -lphysiologyedge
)