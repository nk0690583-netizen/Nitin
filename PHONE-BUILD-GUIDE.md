# Build Voice Sathi APK using only an Android phone

This project is source code, not an APK yet. The APK must be compiled by GitHub Actions in the cloud. You need a free GitHub account and a browser. The project does not require a paid API key.

## 1. Make a GitHub repository
1. Download `VoiceSathi-Android-Project.zip` to your phone.
2. In Chrome, open https://github.com/new and create a repository named `VoiceSathi`. A private repository is fine.
3. The build workflow must live at `.github/workflows/build-apk.yml` in the repository. GitHub cannot run a workflow that is still buried inside a ZIP file.

## 2. Upload the project files
The easiest phone-only route is to extract the ZIP with your phone's Files app, then upload the *contents of the inner `VoiceSathi` folder* to the repository, keeping the folder structure. In the repository choose **Add file → Upload files**. If your browser's mobile file picker cannot upload folders while preserving directories, use Chrome's **Desktop site** or a file manager that supports selecting multiple files. Do not upload only the ZIP and expect the workflow inside it to run.

Make sure the repository root directly contains `settings.gradle`, `build.gradle`, `app/`, and `.github/workflows/build-apk.yml`.

## 3. Start the cloud build
1. Open the repository's **Actions** tab.
2. Select **Build Voice Sathi APK**.
3. Tap **Run workflow** and confirm.
4. Open the running workflow and wait for it to finish. A green check means the cloud build completed; a red cross means it failed and the build log will show why.
5. On the completed run, find **Artifacts** and download `VoiceSathi-debug-apk`. Extract the downloaded artifact ZIP; it contains `app-debug.apk`.
6. Tap the APK to install. Android may ask you to allow installs from that browser or Files app. Only enable that permission for the app you trust, and turn it off again afterwards if desired.

## Notes
- The APK is a debug build, not a Play Store release.
- The phone must have compatible Hindi and US English TTS voice data installed. The app lists offline voices exposed by the phone's TTS engine.
- Voice quality is not guaranteed to match premium cloud services or the four sample clips. The samples are not cloned.
- This app adds no character quota, but Android TTS engine limits, device resources, storage, and audio format compatibility still apply.
- Never share your GitHub password or personal access token in chat.
