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
# Only needed if the SDK isn't installed to a standard system path
# (i.e. you're pointing at a local checkout instead of the apt-installed
# libsmartspectra-dev/libphysiologyedge-dev packages). If it was installed
# via the Presage PPA, this is usually unnecessary — remove it if
# find_package below succeeds without it.
if(DEFINED ENV{SMARTSPECTRA_SDK_PATH})
    list(APPEND CMAKE_PREFIX_PATH "$ENV{SMARTSPECTRA_SDK_PATH}")
endif()

find_package(SmartSpectra CONFIG REQUIRED)
find_package(nlohmann_json REQUIRED)

add_executable(presage-client main.cpp)

target_include_directories(presage-client PRIVATE /usr/include)

target_link_libraries(presage-client
    PRIVATE
        SmartSpectra::SDK
        nlohmann_json::nlohmann_json
        pthread
)