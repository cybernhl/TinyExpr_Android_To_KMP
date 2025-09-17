cmake_minimum_required(VERSION 3.22.1)
project(tinyexpr C)# Project name
# --- 重要設定 ---
# 停用 Xcode Scheme 生成，因為我們只關心函式庫本身
set(CMAKE_XCODE_GENERATE_SCHEME OFF)
# 確保 C 標準，以獲得良好相容性
set(CMAKE_C_STANDARD 99)
set(CMAKE_C_STANDARD_REQUIRED ON)

# --- 建立靜態函式庫 (*.a) ---

# 使用 glob 自動尋找所有位於 'src' 目錄下的 .c 檔案
# 注意：若新增/刪除 .c 檔，需要重新生成 CMake 快取 (例如 ./gradlew clean)
file(GLOB SOURCES "src/*.c")

set(TINYEXPR_SOURCES
        "../../../native/include/tinyexpr.c"
)

#get_filename_component(SHARED_ROOT_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../../../" ABSOLUTE)
#set(NATIVE_CODE_BASE_DIR "${SHARED_ROOT_PATH}/native")
#set(TINYEXPR_SOURCES
#        "${NATIVE_CODE_BASE_DIR}/include/tinyexpr.c"
#)

# 定義一個名為 tinyexpr 的 STATIC (靜態) 函式庫
# 這將會生成 libtinyexpr.a
#add_library(tinyexpr STATIC ${SOURCES})
add_library(tinyexpr STATIC ${TINYEXPR_SOURCES})

# Specify output directory for the library archives (like .a)
set_target_properties(tinyexpr PROPERTIES
        ARCHIVE_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/lib"
        # For shared libraries, it would be LIBRARY_OUTPUT_DIRECTORY
        # For executables, RUNTIME_OUTPUT_DIRECTORY
)



# 將 include 目錄加入標頭檔搜尋路徑
# PUBLIC 表示任何連結到 tinyexpr 的目標也會自動包含這個路徑

target_include_directories(tinyexpr PUBLIC "../../../native/include")

#target_include_directories(tinyexpr PUBLIC
#        "${NATIVE_CODE_BASE_DIR}/include"
#)

# --- 安裝設定 (可選，但建議) ---
# 這定義了執行 'cmake --install' 時，檔案會被安裝到哪裡
# 我們的 Gradle 腳本會使用這個來找到最終的產出物
install(TARGETS tinyexpr
        LIBRARY DESTINATION lib
        ARCHIVE DESTINATION lib
)
install(FILES include/tinyexpr.h DESTINATION include)
message(STATUS "CMake (iosMain/jni): Configuring 'tinyexpr' for target architecture (set externally). Output to lib/ subfolder.")
message(STATUS "CMake (iosMain/jni): CMAKE_C_FLAGS='${CMAKE_C_FLAGS}'")
message(STATUS "CMake (iosMain/jni): CMAKE_OSX_ARCHITECTURES='${CMAKE_OSX_ARCHITECTURES}'")
message(STATUS "CMake (iosMain/jni): CMAKE_OSX_DEPLOYMENT_TARGET='${CMAKE_OSX_DEPLOYMENT_TARGET}'")
message(STATUS "CMake (iosMain/jni): CMAKE_SYSTEM_NAME='${CMAKE_SYSTEM_NAME}'")
message(STATUS "CMake (iosMain/jni): CMAKE_OSX_SYSROOT='${CMAKE_OSX_SYSROOT}'")