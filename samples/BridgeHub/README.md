# Bridge Hub App

Bridge Hub is a starter Android app for turning Meta glasses into camera inputs for downstream systems:
- live video frame bridge (transport integration point)
- snapshot capture and share attachment flow for vision-capable LLM apps

## Current MVP features

- Connect to Meta AI glasses
- Stream camera feed from the device
- Capture photos from glasses
- One-tap "capture and share" for handing images to another app
- FileProvider attachment sharing compatible with LLM-capable mobile apps
- `FrameBridge` hook for forwarding frames to custom transports (WebRTC/RTMP/HTTP/etc.)

## Architecture notes

- DAT registration and stream session logic live in `WearablesViewModel` and `StreamViewModel`.
- Snapshot handoff is isolated in `PhotoAttachmentBridge`.
- Live frame forwarding hook is isolated in `FrameBridge` with a `FrameBridgeSink` interface.
- Current `LoggingFrameBridgeSink` is a placeholder sink; replace this with your real publisher.

## Next implementation targets

1. Add a concrete `FrameBridgeSink` for your transport (WebRTC, RTMP, SRT, or MJPEG endpoint).
2. Add per-destination share presets (package pinning, custom MIME payloads).
3. Add direct API upload flow (OpenAI/other LLMs) alongside Android share-sheet handoff.
4. Add background/foreground service controls for long-running stream bridges.

## Prerequisites

- Android Studio Arctic Fox (2021.3.1) or newer
- JDK 11 or newer
- Android SDK 31+ (Android 12.0+)
- Meta Wearables Device Access Toolkit (included as a dependency)
- A Meta AI glasses device for testing (optional for development)

## Building the app

### Using Android Studio

1. Clone this repository
1. Open the project in Android Studio
1. Add your personal access token (classic) to the `local.properties` file (see [SDK for Android setup](https://wearables.developer.meta.com/docs/getting-started-toolkit/#sdk-for-android-setup))
1. Click **File** > **Sync Project with Gradle Files**
1. Click **Run** > **Run...** > **app**

## Running the app

1. Turn 'Developer Mode' on in the Meta AI app.
1. Launch the app.
1. Press the "Connect" button to complete app registration.
1. Once connected, the camera stream from the device will be displayed
1. Use the on-screen controls to:
   - Capture photos
   - View and save captured photos
   - Disconnect from the device

## Troubleshooting

For issues related to the Meta Wearables Device Access Toolkit, please refer to the [developer documentation](https://wearables.developer.meta.com/docs/develop/) or visit our [discussions forum](https://github.com/facebook/meta-wearables-dat-android/discussions)

## License

This source code is licensed under the license found in the LICENSE file in the root directory of this source tree.
