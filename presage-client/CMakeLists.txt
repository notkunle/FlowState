cmake_minimum_required(VERSION 3.22.1)
project(presage_client CXX)

# SMARTSPECTRA_SDK_PATH must point at the folder you extracted the
# smartspectra-sdk-<version>-windows-x64.zip into, e.g. C:\SmartSpectra
set(SMARTSPECTRA_SDK_DIR "$ENV{SMARTSPECTRA_SDK_PATH}")
if (SMARTSPECTRA_SDK_DIR STREQUAL "")
    message(FATAL_ERROR "SMARTSPECTRA_SDK_PATH is not set. Set it to the folder you extracted the SmartSpectra Windows SDK zip into.")
endif ()

list(APPEND CMAKE_PREFIX_PATH "${SMARTSPECTRA_SDK_DIR}")
find_package(SmartSpectra CONFIG REQUIRED)

add_executable(presage-client main.cpp)
target_link_libraries(presage-client SmartSpectra::SDK)

# Set the standard on the TARGET directly, after find_package(), so it can't
# be silently clobbered by CMAKE_CXX_STANDARD getting reset while the SDK's
# own SmartSpectraConfig.cmake runs (which is what was happening before —
# the compile line kept showing -std:c++17 despite the variable being set
# to 20 earlier in this file).
set_target_properties(presage-client PROPERTIES
    CXX_STANDARD 20
    CXX_STANDARD_REQUIRED ON
)