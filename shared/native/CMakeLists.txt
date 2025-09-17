cmake_minimum_required(VERSION 3.22.1)

project(tinyexpr)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

set(COMMON_NATIVE_SOURCES
        tinyexpr_jni.c
        include/tinyexpr.c
)
set(COMMON_NATIVE_HEADERS
        include/tinyexpr.h
)

# --- 檢測是否為 Android 編譯 ---
# ANDROID_ABI 變數由 Android NDK 的 CMake 工具鏈設定
if (DEFINED ANDROID_ABI AND ANDROID_ABI)
    message(STATUS "Top-level CMake: Detected Android ABI: ${ANDROID_ABI}")
    message(STATUS "Top-level CMake: Configuring for Android platform.")

    # 呼叫位於 src/main/jni/ 目錄下的 CMakeLists.txt
    # CMAKE_CURRENT_SOURCE_DIR 是 shared/native/
    # 我們需要進入 src/main/jni/
    # 注意：add_subdirectory 會在指定的目錄中尋找 CMakeLists.txt 並執行它
    # 它還會創建一個新的建置範圍 (scope)
    # 傳遞給 add_subdirectory 的第二個參數是二進位目錄 (binary_dir)，
    # 通常是相對於當前建置樹的。
    # 如果 Android 的 CMakeLists.txt 需要在特定的建置目錄下操作，
    # 或者如果它期望 CMAKE_CURRENT_SOURCE_DIR 是它自己的位置，這裡需要小心處理。

    # 為了讓 src/main/jni/CMakeLists.txt 中的 CMAKE_CURRENT_SOURCE_DIR 正確
    # 指向其自身 (shared/src/main/jni/)，我們直接使用 add_subdirectory 指向那個目錄。
    # CMake 會自動在那裡尋找 CMakeLists.txt。
    # 該子目錄的建置檔案會產生在 CMAKE_CURRENT_BINARY_DIR 下的一個對應子目錄中。

    set(ANDROID_CMAKE_SUBDIR_PATH "../src/androidMain/jni") # 相對於 native/ 的路徑

    set(ANDROID_CMAKE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/${ANDROID_CMAKE_SUBDIR_PATH}")
    # 或者，更明確的寫法，如果 CMAKE_CURRENT_SOURCE_DIR 就是 native/ 所在的目錄：
    # get_filename_component(PARENT_DIR ${CMAKE_CURRENT_SOURCE_DIR} DIRECTORY) # PARENT_DIR is shared/
    # set(ANDROID_CMAKE_DIR "${PARENT_DIR}/src/androidMain/jni")

    message(STATUS "Top-level CMake: Attempting to add Android subdirectory at: ${ANDROID_CMAKE_DIR}")

    if(IS_DIRECTORY "${ANDROID_CMAKE_DIR}")
        # 檢查該目錄下是否有 CMakeLists.txt
        if(EXISTS "${ANDROID_CMAKE_DIR}/CMakeLists.txt")
            message(STATUS "Top-level CMake: Found CMakeLists.txt in ${ANDROID_CMAKE_DIR}")
            add_subdirectory("${ANDROID_CMAKE_DIR}" android_build)
            message(STATUS "Top-level CMake: Successfully added Android subdirectory: ${ANDROID_CMAKE_DIR}")
        else()
            message(FATAL_ERROR "Top-level CMake: CMakeLists.txt NOT found in directory: ${ANDROID_CMAKE_DIR}")
        endif()
    else()
        message(FATAL_ERROR "Top-level CMake: Android CMakeLists.txt directory NOT found at: ${ANDROID_CMAKE_DIR}")
    endif()

else ()
    message(STATUS "Top-level CMake: Detected Non-Android platform (or ANDROID_ABI not defined).")
    message(STATUS "Top-level CMake: Placeholder for Desktop/Other platform native build.")
    # --- Call CMakeLists.txt in src/desktopMain/jni/ ---
    # CMAKE_CURRENT_SOURCE_DIR is shared/native/
    # The path to desktopMain/jni/ is ../src/desktopMain/jni/

    set(DESKTOP_CMAKE_SUBDIR_PATH "../src/desktopMain/jni") # Relative path from native/
    set(DESKTOP_CMAKE_DIR "${CMAKE_CURRENT_SOURCE_DIR}/${DESKTOP_CMAKE_SUBDIR_PATH}")

    message(STATUS "Top-level CMake (native/): Attempting to add Desktop subdirectory at: ${DESKTOP_CMAKE_DIR}")

    if(IS_DIRECTORY "${DESKTOP_CMAKE_DIR}")
        if(EXISTS "${DESKTOP_CMAKE_DIR}/CMakeLists.txt")
            message(STATUS "Top-level CMake (native/): Found CMakeLists.txt in ${DESKTOP_CMAKE_DIR}")
            # When add_subdirectory is called:
            # 1. CMake processes the CMakeLists.txt in DESKTOP_CMAKE_DIR.
            # 2. Inside that sub-CMakeLists.txt, CMAKE_CURRENT_SOURCE_DIR will correctly be
            #    shared/src/desktopMain/jni/.
            # 3. The build outputs for this sub-project will go into a subdirectory
            #    (e.g., desktop_build_from_native) within the main build directory.
            add_subdirectory("${DESKTOP_CMAKE_DIR}" desktop_build_from_native) # Binary dir for this sub-project
            message(STATUS "Top-level CMake (native/): Successfully added Desktop subdirectory: ${DESKTOP_CMAKE_DIR}")
        else()
            message(FATAL_ERROR "Top-level CMake (native/): CMakeLists.txt NOT found in Desktop directory: ${DESKTOP_CMAKE_DIR}")
        endif()
    else()
        message(FATAL_ERROR "Top-level CMake (native/): Desktop CMakeLists.txt directory NOT found at: ${DESKTOP_CMAKE_DIR}")
    endif()
endif ()

message(STATUS "Top-level CMake: Configuration finished.")