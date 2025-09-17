cmake_minimum_required(VERSION 3.22.1)
project(tinyexpr_jni_for_desktop) # Project name

# Set C language standard
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)
find_package(JNI)
include_directories(${JNI_INCLUDE_DIRS})
# --- Define paths to sources and includes relative to this CMakeLists.txt ---
# CMAKE_CURRENT_SOURCE_DIR is shared/src/desktopMain/jni/
# We need to go up THREE levels to reach the 'shared' directory, then into 'native/'

get_filename_component(PROJECT_ROOT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../../../" ABSOLUTE)
# PROJECT_ROOT_PATH should now be the absolute path to your 'shared' directory
# (Assuming 'shared' is the root of this specific native build context for desktop JNI,
# or the directory containing the 'native' folder as a direct child)

set(NATIVE_CODE_BASE_DIR "${PROJECT_ROOT_PATH}/native")

# List your JNI wrapper source file
set(JNI_WRAPPER_SOURCE "${NATIVE_CODE_BASE_DIR}/tinyexpr_jni.c")

# List your core library source file
set(CORE_LIB_SOURCE "${NATIVE_CODE_BASE_DIR}/include/tinyexpr.c")

# Combine all source files
set(ALL_SOURCES
        ${JNI_WRAPPER_SOURCE}
        ${CORE_LIB_SOURCE}
)

# You can add messages to verify paths during CMake configuration:
message(STATUS "CMAKE_CURRENT_SOURCE_DIR: ${CMAKE_CURRENT_SOURCE_DIR}")
message(STATUS "PROJECT_ROOT_PATH: ${PROJECT_ROOT_PATH}")
message(STATUS "NATIVE_CODE_BASE_DIR: ${NATIVE_CODE_BASE_DIR}")
message(STATUS "JNI Wrapper Source: ${JNI_WRAPPER_SOURCE}")
message(STATUS "Core Lib Source: ${CORE_LIB_SOURCE}")
message(STATUS "All Sources: ${ALL_SOURCES}")

# Define the library target
add_library(
        tinyexpr_jni # Logical target name for your JNI library
        SHARED       # Build as a shared library (.dll, .so, .dylib)
        ${ALL_SOURCES}
)

# Specify include directories for the target
# The compiler needs to find tinyexpr.h
target_include_directories(
        tinyexpr_jni
        PUBLIC
        "${NATIVE_CODE_BASE_DIR}/include"  # Path to the directory containing tinyexpr.h
)

# Optional: If you need to link against other system libraries
# find_library(MATH_LIBRARY m)
# if(MATH_LIBRARY)
#    target_link_libraries(tinyexpr_jni PRIVATE ${MATH_LIBRARY})
# endif()


# Optional: If you need to link against other system libraries (e.g., math library 'm' on Linux/macOS)
# find_library(MATH_LIBRARY m)
# if(MATH_LIBRARY)
#    target_link_libraries(tinyexpr_jni PRIVATE ${MATH_LIBRARY})
# endif()

# Ensure correct output name if necessary (usually handled by Gradle + System.mapLibraryName)
# set_target_properties(tinyexpr_jni PROPERTIES OUTPUT_NAME "tinyexpr_jni")
