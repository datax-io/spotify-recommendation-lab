# Mobile application for Decentralized ML POC: Spotify Recommendation

This repo contains mobile applications to perform training on Spotify listening history.

The applications (Android / iOS) share code from a Kotlin/Native library under `shared`.

The Android application currently does not contain code to actually perform learning.

## Setup (iOS)

You will need the following:

- JDK & Gradle (for building shared library)
- Xcode 13+
- CocoaPods: 
  `sudo gem install cocoapods`
- cocoapods-generate plugin:
  `sudo gem install cocoapods-generate`

1. In `shared/src`, run `symlink.sh` to link `iosX64Main` as `iosArm64Main`
2. In `shared`, sync Gradle.
3. In `ios`, run `pod install`
4. Open `ios/App.xcworkspace` to run the app
