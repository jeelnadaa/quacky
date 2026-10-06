# Compiling Quacky from Source

This guide explains step-by-step how to clone, configure, compile, test, and install the **Quacky** Android APK on Windows, macOS, or Linux.

---

## 📋 Prerequisites

Before compiling, ensure you have the following installed on your machine:

1. **Java Development Kit (JDK)**:
   - **Version:** JDK 17 or JDK 21 (recommended: [Eclipse Temurin](https://adoptium.net/) or OpenJDK).
   - Verify by running:
     ```bash
     java -version
     ```
   - Ensure the `JAVA_HOME` environment variable points to your JDK installation.

2. **Android SDK**:
   - **Target SDK:** 35 (Android 15)
   - **Compile SDK:** 35
   - **Minimum SDK:** 26 (Android 8.0 Oreo)
   - **Build Tools:** 34.0.0 or 35.0.0
   - **Platforms:** `android-35`
   - You can obtain the SDK either via [Android Studio](https://developer.android.com/studio) or through the standalone [Android command-line tools (`cmdline-tools`)](https://developer.android.com/tools).

3. **Git**:
   - Any modern version of Git.

---

## 🚀 Step 1: Clone the Repository

Open your terminal or PowerShell and clone the project:

```bash
git clone https://github.com/jeelnadaa/quacky.git
cd quacky
```

---

## ⚙️ Step 2: Configure Android SDK Location

If you haven't set `ANDROID_HOME` or `ANDROID_SDK_ROOT` in your environment variables, create a file named `local.properties` in the project root directory.

### On Windows:
Create `local.properties` with the following content (adjust to your Windows username):
```properties
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```
*(Notice the escaped colon `\:` and double backslashes `\\`)*

### On macOS:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### On Linux:
```properties
sdk.dir=/home/YOUR_USERNAME/Android/Sdk
```

---

## 🛠️ Step 3: Compile the Debug APK

The repository includes the Gradle wrapper (`gradlew` / `gradlew.bat`), so you do **not** need to install Gradle manually.

### On Windows (PowerShell or Command Prompt):
```powershell
.\gradlew.bat assembleDebug
```

### On macOS / Linux:
```bash
chmod +x gradlew
./gradlew assembleDebug
```

### 📍 Where to find the generated APK:
Once the build completes (`BUILD SUCCESSFUL`), your APK will be located at:
```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 Step 4: Install the APK on a Physical Device or Emulator

Ensure your phone has **Developer Options** and **USB Debugging** enabled, then connect it via USB.

### Using ADB:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Manual Installation:
1. Copy `app-debug.apk` directly to your phone via USB, Google Drive, or local file sharing.
2. Open the file on your device and tap **Install** (allow "Install unknown apps" if prompted).

> [!NOTE]
> **Hardware Features:**
> - **Camera tools** (QR Scanner, Document Scanner, Color Eyedropper) require a physical camera.
> - **AR Ruler** requires a device that supports Google Play Services for AR (ARCore).
> - On devices without ARCore or a camera, Quacky adheres strictly to **Section 5A Hardware Honesty** and presents an explanation screen instead of crashing or faking readings.

---

## 🧪 Step 5: Run Automated Tests

To run all unit tests—including the automated verification that guarantees **zero network permissions** (`android.permission.INTERNET`):

### On Windows:
```powershell
.\gradlew.bat testDebugUnitTest
```

### On macOS / Linux:
```bash
./gradlew testDebugUnitTest
```

### Test Report:
Detailed HTML test results will be generated at:
```text
app/build/reports/tests/testDebugUnitTest/index.html
```

---

## 📦 Step 6: Building a Signed Release APK / AAB (Optional)

To create an optimized release APK or Google Play App Bundle (AAB):

### 1. Generate a Keystore (if you don't already have one):
```bash
keytool -genkey -v -keystore quacky-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias quacky
```

### 2. Configure Signing in `app/build.gradle.kts`:
Add your signing credentials:
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("path/to/quacky-release.jks")
            storePassword = "YOUR_STORE_PASSWORD"
            keyAlias = "quacky"
            keyPassword = "YOUR_KEY_PASSWORD"
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

### 3. Build the Release Bundle:
```bash
# To generate a signed APK:
./gradlew assembleRelease

# To generate an Android App Bundle (.aab) for Google Play:
./gradlew bundleRelease
```

---

## 🔧 Troubleshooting

| Issue | Cause | Solution |
|---|---|---|
| `SDK location not found` | Gradle cannot find the Android SDK | Create `local.properties` with `sdk.dir=...` (see Step 2) or set the `ANDROID_HOME` environment variable. |
| `JAVA_HOME is not set` | Missing or unconfigured JDK path | Install JDK 17 or 21 and export `JAVA_HOME` pointing to its root directory. |
| `Permission denied: ./gradlew` (macOS/Linux) | Script execution bit not set | Run `chmod +x gradlew`. |
| `OutOfMemoryError` during compilation | Gradle daemon memory limit | Add `org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m` to `gradle.properties`. |
| Font fallback warning | `Satoshi` font files not populated | This is intentional by design. The app automatically falls back to clean system sans-serif without failing the build. |

---

## 🔒 100% Offline Privacy Guarantee

Quacky does not declare `android.permission.INTERNET` or any network capabilities. When compiling the project yourself, you can verify this directly in:
- [AndroidManifest.xml](file:///d:/quacky-app/app/src/main/AndroidManifest.xml)
- [OfflineManifestTest.kt](file:///d:/quacky-app/app/src/test/java/app/quacky/core/OfflineManifestTest.kt)
