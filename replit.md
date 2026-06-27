# VCamera

## Project Overview

VCamera is a native **Android application** (Kotlin + Java) that virtualizes the camera system, allowing users to replace the real camera feed with a video file, image, or network stream. Use cases include:

- Live broadcast assistant (stream local video to Twitch, YouTube, Facebook, etc.)
- Camera privacy protection (prevent apps from taking unauthorized photos)
- Entertainment (prank friends with pre-recorded video)

## Architecture

- **Type:** Android mobile app — this is NOT a web application and cannot run as a web server in Replit
- **Build System:** Gradle (AGP 7.0.2)
- **Languages:** Kotlin (primary), Java (utilities), C++ (native env detection)
- **UI Pattern:** MVVM with LiveData, Kotlin Coroutines, ViewBinding
- **Modules:**
  - `:app` — main application
  - `:opensdk` — virtual machine SDK (HackApi, HackApplication) for intercepting camera calls

## Key Features

1. Replace camera with a photo
2. Replace camera with a local video
3. Replace camera with a network video stream
4. Resize, rotate, move, zoom, flip the injected video

## Building

To build this project you need Android Studio or the Android SDK command-line tools installed locally. This project cannot be built or run in the Replit web environment.

```bash
./gradlew assembleDebug
```

Minimum SDK: 24 | Target SDK: 31 | Compile SDK: 33

## Links

- [Google Play](https://play.google.com/store/apps/details?id=virtual.camera.app)
- [Demo Video](https://www.youtube.com/embed/lT-MP9c7SbY)
- Contact: andvipgroup@gmail.com

## User Preferences

(none yet)
