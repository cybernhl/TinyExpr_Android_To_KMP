# TinyExpr Android to KMP

[繁體中文](#繁體中文) | [English](#english)

---

## 繁體中文

### 專案簡介
本專案目標是研究 **Kotlin Multiplatform (KMP)** 如何在 **Android / JVM / iOS** 平台中共用 **C / JNI** 函式庫。

範例使用了 [tinyexpr](https://github.com/codeplea/tinyexpr) 這個輕量級 C 語言數學表達式解析器，並透過 **Gradle + CMake** 自動化編譯，嘗試產生各平台可用的二進位檔案。

原始參考文章與專案：
- 📖 [KMP Native C/C++ by Adman Shadman](https://medium.com/@adman.shadman/kmp-native-c-c-a7b62e4e9c1f)
- 💻 [shadmanadman/KMP-TinyExpr](https://github.com/shadmanadman/KMP-TinyExpr)

與其將 C 原始碼手動複製，這裡改用 **Git Submodule** 的方式引入 tinyexpr，方便維護與更新。

---

### 特色
- 🌐 Kotlin Multiplatform: Android / JVM / iOS
- 🔗 使用 Git Submodule 引入 [tinyexpr](https://github.com/codeplea/tinyexpr)
- ⚙️ 使用 Gradle 與 CMake 建置各平台二進位
- 📦 範例 JNI 封裝，提供 JVM 與 Android 使用
- 🍏 iOS 平台輸出 `.a` 靜態函式庫

---

### 專案結構
```plaintext
TinyExpr_Android_To_KMP/
 ├─ shared/                # KMP 共用模組
 │   ├─ build.gradle.kts   # Gradle 與 CMake 設定
 │   ├─ src/               # Kotlin 共用程式碼
 │   └─ native/            # CMake 輸出的產物
 ├─ tinyexpr/              # 透過 Git Submodule 引入
 └─ ...
```

---

### 建置方式

#### 1. 下載專案（包含 Submodule）
```bash
git clone --recurse-submodules https://github.com/cybernhl/TinyExpr_Android_To_KMP.git
```

若已經 clone 過，請執行：
```bash
git submodule update --init --recursive
```

#### 2. 編譯
專案會依照平台輸出對應的二進位檔案：
- Android → `.so`
- JVM → `.dylib` / `.so`
- iOS → `.a`

可直接透過 **Gradle** 進行建置：
```bash
./gradlew build
```

---

### 參考
- [tinyexpr 原始碼](https://github.com/codeplea/tinyexpr)
- [KMP-TinyExpr 範例](https://github.com/shadmanadman/KMP-TinyExpr)
- [KMP Native C/C++ 教學文章](https://medium.com/@adman.shadman/kmp-native-c-c-a7b62e4e9c1f)

---

## English

### Overview
This project demonstrates how **Kotlin Multiplatform (KMP)** can integrate **C / JNI** libraries across **Android / JVM / iOS** platforms.

We use [tinyexpr](https://github.com/codeplea/tinyexpr), a lightweight C math expression parser, and build it via **Gradle + CMake** to generate native binaries for each platform.

Original references:
- 📖 [KMP Native C/C++ by Adman Shadman](https://medium.com/@adman.shadman/kmp-native-c-c-a7b62e4e9c1f)
- 💻 [shadmanadman/KMP-TinyExpr](https://github.com/shadmanadman/KMP-TinyExpr)

Instead of manually copying source files, this project uses a **Git Submodule** to include tinyexpr, making updates easier.

---

### Features
- 🌐 Kotlin Multiplatform: Android / JVM / iOS
- 🔗 Include [tinyexpr](https://github.com/codeplea/tinyexpr) via Git Submodule
- ⚙️ Build native binaries with Gradle + CMake
- 📦 JNI wrapper example for JVM / Android
- 🍏 iOS static library (`.a`) support

---

### Project Structure
```plaintext
TinyExpr_Android_To_KMP/
 ├─ shared/                # KMP shared module
 │   ├─ build.gradle.kts   # Gradle & CMake configuration
 │   ├─ src/               # Shared Kotlin code
 │   └─ native/            # CMake build outputs
 ├─ tinyexpr/              # Git submodule
 └─ ...
```

---

### Build Instructions

#### 1. Clone repository with submodules
```bash
git clone --recurse-submodules https://github.com/cybernhl/TinyExpr_Android_To_KMP.git
```

If already cloned:
```bash
git submodule update --init --recursive
```

#### 2. Build
The project generates binaries for different targets:
- Android → `.so`
- JVM → `.dylib` / `.so`
- iOS → `.a`

Run Gradle build:
```bash
./gradlew build
```

---

### References
- [tinyexpr source](https://github.com/codeplea/tinyexpr)
- [KMP-TinyExpr example](https://github.com/shadmanadman/KMP-TinyExpr)
- [KMP Native C/C++ tutorial](https://medium.com/@adman.shadman/kmp-native-c-c-a7b62e4e9c1f)  
