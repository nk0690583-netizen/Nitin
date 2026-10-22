# Voice Sathi — Android TTS starter app

An Android starter app for creating long Hindi and US-English narration using the phone's installed Android Text-to-Speech engine. It splits long scripts into smaller parts and combines generated WAV parts into one WAV file for export.

## Features
- Hindi (India) and English (United States) language selection
- Lists offline voices installed on the phone for the selected language
- Adjustable speaking speed
- Long scripts split into chunks and synthesized sequentially
- Attempts to combine compatible PCM/WAV chunks and exports via Android's document picker
- No API key or paid cloud service required for supported offline voices

## Important limitations
- This is source code, not a prebuilt APK.
- Voice quality and available voices depend on the TTS engine and voice packs installed on the phone. Premium-quality neural voices and unlimited server-grade output cannot be guaranteed.
- WAV combination expects standard RIFF/WAVE output with matching audio formats; some TTS engines may be incompatible.
- “Unlimited” means no app-imposed character quota. Phone memory, storage, generation time, TTS engine limits, and Android behavior still apply.
- The four supplied audio clips are style references only. This app does not clone or imitate a speaker's identity.

## Build without a computer
Follow [PHONE-BUILD-GUIDE.md](PHONE-BUILD-GUIDE.md) to compile a debug APK through GitHub Actions from an Android phone. The GitHub workflow is in `.github/workflows/build-apk.yml`.

## Build on a computer
Open this folder in Android Studio, allow Gradle sync, install Android SDK Platform 35 if requested, and build the debug APK.
