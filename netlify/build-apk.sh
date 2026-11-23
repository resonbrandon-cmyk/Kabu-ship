#!/usr/bin/env bash
# Builds the kabu-ship Android debug APK on Netlify and writes a small download
# page next to it. This mirrors .github/workflows/release-apk.yml.
#
# The Netlify build image ships no Android SDK or Gradle (and a newer JDK than
# the app is tested with), so the toolchain is downloaded into $HOME (outside
# the repo).
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT_DIR="${REPO_DIR}/build/netlify"
TOOLS_DIR="${HOME}/.kabu-ship-toolchain"

GRADLE_VERSION="9.3.1"
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
SDK_PACKAGES=("platform-tools" "platforms;android-36.1" "build-tools;36.0.0")

mkdir -p "${TOOLS_DIR}"

# --- JDK 21 (same as the GitHub workflow) -----------------------------------
JDK_DIR="${TOOLS_DIR}/jdk-21"
if [ ! -x "${JDK_DIR}/bin/java" ]; then
  echo "==> Downloading Temurin JDK 21"
  mkdir -p "${JDK_DIR}"
  curl -fsSL "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    | tar -xz -C "${JDK_DIR}" --strip-components=1
fi
export JAVA_HOME="${JDK_DIR}"
export PATH="${JAVA_HOME}/bin:${PATH}"

# --- Gradle -----------------------------------------------------------------
GRADLE_HOME_DIR="${TOOLS_DIR}/gradle-${GRADLE_VERSION}"
if [ ! -x "${GRADLE_HOME_DIR}/bin/gradle" ]; then
  echo "==> Downloading Gradle ${GRADLE_VERSION}"
  curl -fsSL -o "${TOOLS_DIR}/gradle.zip" \
    "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
  unzip -q -o "${TOOLS_DIR}/gradle.zip" -d "${TOOLS_DIR}"
  rm -f "${TOOLS_DIR}/gradle.zip"
fi
export PATH="${GRADLE_HOME_DIR}/bin:${PATH}"

# --- Android SDK ------------------------------------------------------------
export ANDROID_HOME="${TOOLS_DIR}/android-sdk"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
SDKMANAGER="${ANDROID_HOME}/cmdline-tools/latest/bin/sdkmanager"
if [ ! -x "${SDKMANAGER}" ]; then
  echo "==> Downloading Android command-line tools"
  mkdir -p "${ANDROID_HOME}/cmdline-tools"
  curl -fsSL -o "${TOOLS_DIR}/cmdline-tools.zip" \
    "https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP}"
  unzip -q -o "${TOOLS_DIR}/cmdline-tools.zip" -d "${ANDROID_HOME}/cmdline-tools"
  rm -rf "${ANDROID_HOME}/cmdline-tools/latest"
  mv "${ANDROID_HOME}/cmdline-tools/cmdline-tools" "${ANDROID_HOME}/cmdline-tools/latest"
  rm -f "${TOOLS_DIR}/cmdline-tools.zip"
fi
echo "==> Installing Android SDK packages"
yes | "${SDKMANAGER}" --licenses > /dev/null || true
"${SDKMANAGER}" --install "${SDK_PACKAGES[@]}" > /dev/null

# --- Debug keystore (same as the GitHub workflow) ---------------------------
cd "${REPO_DIR}"
if [ ! -f debug.keystore ]; then
  if [ -f debug.keystore.base64 ]; then
    base64 -d debug.keystore.base64 > debug.keystore
  else
    keytool -genkeypair -keystore debug.keystore -storepass android \
      -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 \
      -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
  fi
fi

# --- Firebase config (google-services.json) ---------------------------------
# The app reads R.string.default_web_client_id (and the other Firebase values),
# which the Google Services Gradle plugin generates from app/google-services.json.
# That file is not in the repo (AI Studio injects it), so the build:
#   1. writes it from the GOOGLE_SERVICES_JSON env var (raw JSON or base64), or
#   2. uses a committed app/google-services.json, or
#   3. falls back to a placeholder so the APK still compiles. Firebase sign-in
#      and data sync won't work in that APK, and the download page says so.
GOOGLE_SERVICES_FILE="app/google-services.json"
FIREBASE_CONFIGURED="yes"
if [ -n "${GOOGLE_SERVICES_JSON:-}" ]; then
  echo "==> Writing ${GOOGLE_SERVICES_FILE} from GOOGLE_SERVICES_JSON"
  if [[ "$(printf '%s' "${GOOGLE_SERVICES_JSON}" | tr -d '[:space:]' | head -c 1)" == "{" ]]; then
    printf '%s' "${GOOGLE_SERVICES_JSON}" > "${GOOGLE_SERVICES_FILE}"
  else
    printf '%s' "${GOOGLE_SERVICES_JSON}" | base64 -d > "${GOOGLE_SERVICES_FILE}"
  fi
  trap 'rm -f "${REPO_DIR}/${GOOGLE_SERVICES_FILE}"' EXIT
elif [ -f "${GOOGLE_SERVICES_FILE}" ]; then
  echo "==> Using committed ${GOOGLE_SERVICES_FILE}"
else
  FIREBASE_CONFIGURED="no"
  echo "WARNING: No Firebase config found (set GOOGLE_SERVICES_JSON)."
  echo "WARNING: Building with a placeholder; Firebase sign-in will not work in this APK."
  APPLICATION_ID="$(sed -n 's/.*applicationId *= *"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1)"
  cat > "${GOOGLE_SERVICES_FILE}" <<JSON
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "placeholder-project",
    "storage_bucket": "placeholder-project.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000",
        "android_client_info": { "package_name": "${APPLICATION_ID}" }
      },
      "oauth_client": [
        { "client_id": "000000000000-placeholder.apps.googleusercontent.com", "client_type": 3 }
      ],
      "api_key": [ { "current_key": "placeholder-api-key" } ],
      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
    }
  ],
  "configuration_version": "1"
}
JSON
  trap 'rm -f "${REPO_DIR}/${GOOGLE_SERVICES_FILE}"' EXIT
fi

# --- Build ------------------------------------------------------------------
echo "==> Building APK"
gradle --no-daemon :app:assembleDebug

# --- Site output ------------------------------------------------------------
rm -rf "${OUT_DIR}"
mkdir -p "${OUT_DIR}"
cp app/build/outputs/apk/debug/app-debug.apk "${OUT_DIR}/kabu-ship.apk"
APK_SIZE_MB="$(du -m "${OUT_DIR}/kabu-ship.apk" | cut -f1)"
BUILT_AT="$(date -u '+%Y-%m-%d %H:%M UTC')"
COMMIT="${COMMIT_REF:-local}"
FIREBASE_NOTICE=""
if [ "${FIREBASE_CONFIGURED}" = "no" ]; then
  FIREBASE_NOTICE='<p class="notice"><strong>Preview build:</strong> this APK was built without the Firebase project config, so Google sign-in and order sync will not work.</p>'
fi

cat > "${OUT_DIR}/index.html" <<HTML
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>kabu-ship for Android</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 34rem; margin: 3rem auto; padding: 0 1.25rem; line-height: 1.5; color: #1a1a1a; }
    a.button { display: inline-block; padding: .8rem 1.4rem; background: #1a1a1a; color: #fff; border-radius: .5rem; text-decoration: none; font-weight: 600; }
    small { color: #666; }
    .notice { padding: .75rem 1rem; background: #fff4e5; border: 1px solid #f0b46c; border-radius: .5rem; }
  </style>
</head>
<body>
  <h1>kabu-ship</h1>
  <p>Track WhatsApp and Instagram orders from DM inquiry and payment to packing, courier dispatch, and doorstep delivery.</p>
  ${FIREBASE_NOTICE}
  <p><a class="button" href="/kabu-ship.apk" download>Download the Android APK (${APK_SIZE_MB} MB)</a></p>
  <ol>
    <li>Download <code>kabu-ship.apk</code> on your Android phone.</li>
    <li>Open the file and allow <strong>Install unknown apps</strong> if prompted.</li>
    <li>Sign in with your Google account.</li>
  </ol>
  <p><small>Built ${BUILT_AT} from commit ${COMMIT:0:7}.</small></p>
</body>
</html>
HTML

echo "==> Site written to ${OUT_DIR}"
