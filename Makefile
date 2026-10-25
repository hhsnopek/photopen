.PHONY: all android install release test lint ktlint detekt format check clean emulator devices version keystore help

# ============================================================================
# Environment (JDK 21, Android SDK 35)
# ============================================================================

export JAVA_HOME ?= /home/hs/opt/jdk21
export ANDROID_HOME ?= /home/hs/AndroidSDK
export ANDROID_SDK_ROOT ?= $(ANDROID_HOME)

ADB ?= $(ANDROID_HOME)/platform-tools/adb
GRADLE ?= ./gradlew
AVD ?= probe35

# Version is read from the build file, the single source of truth. The release
# workflow rewrites both from the git tag before building.
VERSION := $(shell grep -m1 'versionName' app/build.gradle.kts | sed -E 's/.*"([^"]+)".*/\1/')
VERSION_CODE := $(shell grep -m1 'versionCode' app/build.gradle.kts | sed -E 's/[^0-9]*([0-9]+).*/\1/')

APK_DEBUG := app/build/outputs/apk/debug/app-debug.apk
APK_RELEASE := app/build/outputs/apk/release/app-release.apk

# Release signing. CI sets these from repository secrets. Locally they come
# from the environment or from local.properties (gitignored); nothing is
# committed.
ALIAS ?= photopen

all: android

android:
	$(GRADLE) assembleDebug
	@echo "Debug APK: $(APK_DEBUG)"

install: android
	$(ADB) install -r $(APK_DEBUG)

release:
	$(GRADLE) assembleRelease
	@echo "Release APK: $(APK_RELEASE)"

test:
	$(GRADLE) testDebugUnitTest

lint:
	$(GRADLE) lint

ktlint:
	$(GRADLE) ktlintCheck

detekt:
	$(GRADLE) detekt

format:
	$(GRADLE) ktlintFormat

check: ktlint detekt lint test

version:
	@echo "$(VERSION) ($(VERSION_CODE))"

# Creates the keystore the release variant signs with. Back it up: losing it
# means no future release can update an installed copy.
keystore:
	@if [ ! -f app/keystore/release.keystore ]; then \
		echo "Creating release keystore..."; \
		mkdir -p app/keystore; \
		KEYTOOL=$$(command -v keytool || echo "$(JAVA_HOME)/bin/keytool"); \
		: $${KEYSTORE_PASSWORD:?set KEYSTORE_PASSWORD}; \
		: $${KEY_PASSWORD:?set KEY_PASSWORD}; \
		$$KEYTOOL -genkey -v \
			-keystore app/keystore/release.keystore \
			-alias $(ALIAS) \
			-keyalg RSA -keysize 2048 -validity 10000 \
			-storepass:env KEYSTORE_PASSWORD -keypass:env KEY_PASSWORD \
			-dname "CN=PhotoPen, OU=Mobile, O=Personal, L=Unknown, ST=Unknown, C=US"; \
	else \
		echo "app/keystore/release.keystore already exists, leaving it alone"; \
	fi

# Boot the emulator used for verification when no phone is plugged in.
emulator:
	@if $(ADB) devices | grep -q "device$$"; then \
		echo "A device is already connected"; \
	else \
		$(ANDROID_HOME)/emulator/emulator -avd $(AVD) -no-snapshot-save -no-boot-anim & \
		$(ADB) wait-for-device; \
	fi

devices:
	$(ADB) devices -l

clean:
	$(GRADLE) clean

help:
	@grep -E '^[a-zA-Z_-]+:' Makefile | cut -d: -f1 | sort -u
