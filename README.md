# Mobile application for Decentralized ML POC: Spotify Recommendation

This repo contains mobile applications to perform training on Spotify listening history.

The applications (Android / iOS) share code from a Kotlin/Native library under `shared`.

The Android application currently does not contain code to actually perform learning.

## Setup (iOS)

You will need the following:

- JDK & Gradle (for building shared library)
- Xcode 13+ (under Rosetta for M1 Macs)
- CocoaPods: 
  `sudo gem install cocoapods`
- cocoapods-generate plugin:
  `sudo gem install cocoapods-generate`
- A modified version of SwiftSyt:
  Clone `https://github.com/datax-io/swiftsyft-parcel` branch `parcel` to directory `swiftsyft-parcel` right next to this repo root. 
  It is referenced in `ios/Podfile`.

1. In `shared/src`, run `symlink.sh` to link `iosX64Main` as `iosArm64Main`
2. In `shared`, perform Gradle sync.
3. In `ios`, run `pod install`
4. Open `ios/App.xcworkspace` to run the app

## Using the app

1. Configure Spotify client ID and authorize your account
   1. Log into Spotify developer dashboard: https://developer.spotify.com/dashboard/applications
   2. Create an app. Any title would be fine.
   3. On the app detail page, record the Client ID displayed under the name of the app.
   4. Click "Edit Settings" to open the settings dialog. Under "Redirect URIs", add `http://localhost:8888/callback`
   5. Click "Users and Access" and add your own spotify account as a new user.
   6. Inside the app, next to "Spotify" > "Client ID", tap "Change" and paste the Client ID obtained from the developer dashboard.
   7. Tap "Authorize" to test the Spotify authorization.
   8. Tap "Fetch" to retrieve your listening history.
2. Authorize your Oasis account.
   1. Inside the app, tap "Authorize" under "Parcel".
   2. Follow instructions on the screen to authorize the app to interact with Parcel on your behalf.
3. Starting training
   1. Inside the app, tap "Train with API Data".
   2. A new screen should show. Training will start automatically with output similar to this:
```text
Data source: API
Participant ID: 1
Data: 4067 tracks
Model: spotify_recommendation
Version: 1.0
connecting to ws://pygrid.dadtax.io:7001
Loading data, batch size = 64
Training...
Uploading diff...
Uploaded document
<<document-id>>
Reporting diff...
Done
```
