cmake_minimum_required(VERSION 3.18)
# 專案名稱，最終的函式庫名稱將是 'tinyexpr' 以匹配 System.loadLibrary("tinyexpr")
project(tinyexpr_android_module) # 專案名稱可以與函式庫名不同，但 add_library 中會用 'tinyexpr'

# --- 基本 C 設定 ---
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -std=c99")
# 如果 tinyexpr_jni.c 或 tinyexpr.c 中有 C++ 程式碼，才需要 CXX_FLAGS
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

# --- 定義原生程式碼的路徑 (相對於此 CMakeLists.txt) ---
# CMAKE_CURRENT_SOURCE_DIR 是 shared/src/main/jni/
# 我們需要向上三層到 shared/，然後再進入 native/
set(NATIVE_CODE_ROOT_DIR "../../../native")

message(STATUS "CMakeLists.txt is at: ${CMAKE_CURRENT_SOURCE_DIR}")
message(STATUS "Looking for native code in: ${NATIVE_CODE_ROOT_DIR}")

# --- 明確指定來源檔案 ---
set(NATIVE_LIBRARY_SOURCES
        "${NATIVE_CODE_ROOT_DIR}/tinyexpr_jni.c"
        "${NATIVE_CODE_ROOT_DIR}/include/tinyexpr.c"
)

# --- 明確指定標頭檔 (主要用於 IDE 索引和依賴追蹤，對編譯共享庫不是嚴格必需，但良好實踐) ---
# 實際上，target_include_directories 更重要
set(NATIVE_LIBRARY_HEADERS
        "${NATIVE_CODE_ROOT_DIR}/include/tinyexpr.h"
)

# --- 新增函式庫目標 ---
# 函式庫名稱為 "tinyexpr"，以匹配 TinyExprJNI.kt 中的 System.loadLibrary("tinyexpr")
add_library(
        tinyexpr # 這是 Android 端 System.loadLibrary() 期望的名稱
        SHARED
        ${NATIVE_LIBRARY_SOURCES}
        # ${NATIVE_LIBRARY_HEADERS} # 標頭檔通常不需要直接加到 add_library，除非它們是模板或需要特殊處理
)

# --- 設定目標包含目錄 ---
# 允許 tinyexpr_jni.c 能夠 #include "tinyexpr.h"
# 也允許 tinyexpr.c/h 之間可能的內部包含
target_include_directories(
        tinyexpr
        PUBLIC
        # 這個路徑需要讓編譯器找到 tinyexpr.h
        # 當 tinyexpr_jni.c 中 #include "tinyexpr.h" 時，
        # 編譯器會在 -I<path> 中尋找 <path>/tinyexpr.h
        "${NATIVE_CODE_ROOT_DIR}/include"
        # JNI 標頭檔 (jni.h) 的路徑通常由 Android NDK 的 CMake 工具鏈自動處理
)

# --- 可選：設定目標屬性 (如果需要) ---
# set_target_properties(tinyexpr PROPERTIES ...)

message(STATUS "Android CMake (jni): Configuring 'tinyexpr' native library for Android.")
message(STATUS "Android CMake (jni): Source files: ${NATIVE_LIBRARY_SOURCES}")
message(STATUS "Android CMake (jni): Include directory for 'tinyexpr.h': ${NATIVE_CODE_ROOT_DIR}/include")
