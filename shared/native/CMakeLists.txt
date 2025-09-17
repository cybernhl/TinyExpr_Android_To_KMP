cmake_minimum_required(VERSION 3.22.1)
project(TinyExprTopLevelDispatcher LANGUAGES C CXX) # Keep or adjust project name

# Output some initial diagnostic information
message(STATUS "TopLevelCMake: CMAKE_GENERATOR = ${CMAKE_GENERATOR}")
message(STATUS "TopLevelCMake: CMAKE_SYSTEM_NAME = ${CMAKE_SYSTEM_NAME}") # Used for iOS/Desktop
message(STATUS "TopLevelCMake: ANDROID_ABI = ${ANDROID_ABI}")             # Used for Android
message(STATUS "TopLevelCMake: CMAKE_BINARY_DIR (top-level) = ${CMAKE_BINARY_DIR}")
message(STATUS "TopLevelCMake: CMAKE_SOURCE_DIR (top-level) = ${CMAKE_SOURCE_DIR}")

# --- Platform-specific dispatching ---

# For Android, let's use the ANDROID_ABI check, similar to your old working version.
# AGP defines ANDROID_ABI. CMAKE_SYSTEM_NAME will also be "Android".
# Using ANDROID_ABI is a very specific check for Android NDK builds.
if(DEFINED ANDROID_ABI AND ANDROID_ABI)
    message(STATUS "TopLevelCMake: Detected Android build via ANDROID_ABI: ${ANDROID_ABI}")

    # Set common flags if they were essential in the old version and not set by AGP/NDK toolchain
    # AGP/NDK toolchain usually sets appropriate C/CXX standards.
    # If src/androidMain/jni/CMakeLists.txt relies on these being set globally, uncomment them.
    # However, it's better if the sub-CMakeLists.txt is self-contained or AGP handles it.
    # set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99" PARENT_SCOPE) # PARENT_SCOPE might not be what you want here
    # set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11" PARENT_SCOPE)

    set(ANDROID_JNI_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../src/androidMain/jni") # Path relative to this CMakeLists.txt

    if(EXISTS "${ANDROID_JNI_SRC_DIR}/CMakeLists.txt")
        # Use "android_build" as the binary directory name, same as your old version
        add_subdirectory("${ANDROID_JNI_SRC_DIR}" android_build)
        message(STATUS "TopLevelCMake: Added Android subdirectory from ${ANDROID_JNI_SRC_DIR} into 'android_build'")
    else()
        message(FATAL_ERROR "TopLevelCMake: Android CMakeLists.txt not found at ${ANDROID_JNI_SRC_DIR}")
    endif()

elseif(CMAKE_SYSTEM_NAME STREQUAL "iOS") # For iOS, CMAKE_SYSTEM_NAME is the primary check
    message(STATUS "TopLevelCMake: Detected iOS platform.")
    set(IOS_NATIVE_SUBDIR "${CMAKE_CURRENT_SOURCE_DIR}/../src/iosMain/native")
    if(EXISTS "${IOS_NATIVE_SUBDIR}/CMakeLists.txt")
        add_subdirectory("${IOS_NATIVE_SUBDIR}" ios_specific_build_artifacts)
        message(STATUS "TopLevelCMake: Added iOS subdirectory from ${IOS_NATIVE_SUBDIR}. Build will be in 'ios_specific_build_artifacts' folder.")
    else()
        message(FATAL_ERROR "TopLevelCMake: iOS CMakeLists.txt not found at ${IOS_NATIVE_SUBDIR}")
    endif()

elseif(DEFINED DESKTOP_BUILD OR (NOT ANDROID_ABI AND NOT CMAKE_SYSTEM_NAME STREQUAL "iOS") ) # Fallback for Desktop
    # This condition tries to catch desktop if it's not Android and not iOS.
    # Passing -DDESKTOP_BUILD=ON from Gradle for desktop is more reliable.
    message(STATUS "TopLevelCMake: Detected Non-Android/Non-iOS platform, assuming Desktop (or check DESKTOP_BUILD flag).")

    set(DESKTOP_JNI_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/../src/desktopMain/jni")
    if(EXISTS "${DESKTOP_JNI_SRC_DIR}/CMakeLists.txt")
        # Use "desktop_build_from_native" as in your old version, or choose a new one.
        add_subdirectory("${DESKTOP_JNI_SRC_DIR}" desktop_build)
        message(STATUS "TopLevelCMake: Added Desktop subdirectory from ${DESKTOP_JNI_SRC_DIR} into 'desktop_build'")
    else()
        message(FATAL_ERROR "TopLevelCMake: Desktop CMakeLists.txt not found at ${DESKTOP_JNI_SRC_DIR}")
    endif()

else()
    message(WARNING "TopLevelCMake: No specific platform dispatched. ANDROID_ABI='${ANDROID_ABI}', CMAKE_SYSTEM_NAME='${CMAKE_SYSTEM_NAME}'.")
endif()

message(STATUS "TopLevelCMake: Configuration finished.")

