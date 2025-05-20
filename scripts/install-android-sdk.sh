#!/usr/bin/env bash
#
# Copyright (C) 2024-2025 OpenAni and contributors.
#
# 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
# Use of this source code is governed by the GNU AGPLv3 license, which can be found at the following link.
#
# https://github.com/open-ani/ani/blob/main/LICENSE
#

set -euo pipefail

# Base SDK from Debian/Ubuntu repos
apt -qq update
apt -qq install -y --install-recommends android-sdk unzip curl

# --------------------------------------------------------------------
# Environment
export ANDROID_HOME=/usr/lib/android-sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin"

echo "sdk.dir=$ANDROID_HOME" > local.properties

# --------------------------------------------------------------------
# Fetch latest command-line tools (needed for `sdkmanager`)
mkdir -p "$ANDROID_HOME/cmdline-tools"
cd /tmp
curl -sSL https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -o cmdline-tools.zip
rm -rf cmdline-tools
unzip -q cmdline-tools.zip
mv cmdline-tools "$ANDROID_HOME/cmdline-tools/latest"
rm cmdline-tools.zip

# --------------------------------------------------------------------
# Accept licences required by Gradle / Android build-tools
yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null

SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
yes | "$SDKMANAGER" \
    "platforms;android-35" \
    "build-tools;35.0.0-rc1" \
    "platform-tools"

echo "✅ Android SDK API 35 installed."
