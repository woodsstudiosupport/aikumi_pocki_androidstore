# Aikumi Pocket

Woods Studio's Android AI companion. Package: `com.woodsstudio.android.aikumi.pocket`.

This is the first **source foundation**, with planning continuing. It implements a Kotlin startup loader, a remotely served interface, three-screen onboarding, local profile settings, and a chat layout. It does not yet call OpenAI, record audio, send notifications, or send messages. Send is deliberately unavailable; there are no simulated AI replies. The opening greeting is static interface copy.

## Layout

- `android/` — Android shell, bundled Woods Studio logo, gradient spinner, bounded resource downloader, local profile storage, restricted WebView.
- `remote/manifest.json` — runtime release ID, shell compatibility, file sizes and SHA-256 hashes.
- `remote/ui/` — HTML, CSS and JavaScript for onboarding, chat and settings.
- `remote/core/personality.json` — machine-readable base personality derived from the agreed character specification.
- `docs/` — living plan, agreed designs, and implementation notes.
- `tools/` — manifest generation and verification tests.

## Open the Android source

Open `android/` in Android Studio, install SDK platform 35/build tools as requested, and use JDK 17 or newer. The checked-in Gradle wrapper uses Gradle 8.11.1. Then run:

```sh
cd android
./gradlew testDebugUnitTest assembleDebug
```

On Windows use `gradlew.bat testDebugUnitTest assembleDebug`. Run the app on Android 8.0/API 26 or newer with a current Android System WebView. The runtime endpoint is fixed to this repository's `main/remote/` directory. An unauthenticated readable repository is required by this foundation; private-repository authentication has not been implemented. Never put a shared write token in the app.

No release AAB or signing key is generated at this stage. Public-release target SDK and policy readiness must be reviewed before release.

## Updating runtime files

After changing any runtime file:

```sh
python tools/release.py
python tools/release.py --check
```

Commit the resulting manifest **together with** the runtime files. The next app launch checks it and downloads only changed bytes. A hash failure keeps the currently verified cached release. Keep native feature changes in Play-delivered app updates.

This first manifest uses HTTPS and SHA-256 integrity checks, **not publisher signatures**. A trusted repository publisher can change the runtime. Manifest signing and a pinned public key remain a public-release task.

## Interface preview and checks

```sh
npm install
npm test
python tools/release.py --check
python -m http.server 8765 --directory remote
```

Open `http://localhost:8765/ui/` for a browser-only preview. It uses session storage, whereas Android uses app-private preferences through an origin-restricted message bridge. Preview data is neither a real Android trait store nor sent anywhere.

Optional real-browser checks: `npx playwright install chromium`, then `npm run test:browser` while the preview server runs. You can set `CHROMIUM_PATH` to use an existing Chromium executable.

## Personal data and credentials

On Android, name, interests and onboarding choices are stored locally. Backup and device transfer are excluded. The history preference is saved, but history storage is not implemented because chat is not connected yet. Reset removes the current personalisation fields and returns to onboarding. No traits or messages are uploaded to this repository.

The closed-testing build can embed an OpenAI key at compile time. The build configuration does not commit the injected key to source. Normal debug and release variants receive an empty value; only `closedTesting` uses `AIKUMI_TEST_OPENAI_KEY` from the build environment, and that variant refuses to build without it. Native code can access it through `TestingCredentials`; it is not exposed to the web interface.

The private `aikumi_keyhold` repository owns the credential-bearing build workflow and private APK artifact. No GitHub credential is placed in the APK, and no backend or OpenLVT call is needed to read the embedded testing key. The OpenAI request/chat connection is still pending.

For a local closed-testing build, set `AIKUMI_TEST_OPENAI_KEY` in the process environment, then run `./gradlew assembleClosedTesting` in `android/`. Do not put the key on a shell command line, in a committed file, or in public CI artifacts. Embedded keys remain extractable from an APK. Public-release key handling and release signing remain separate work.

## Artwork

The startup logo is the existing linked Woods Studio logo, bundled for offline startup. New art is to be created through Canva. The first Canva output was unsuitable and was not incorporated. The `a` avatar is a temporary text treatment, not a final portrait or app icon. Artwork selection and Play Store graphics remain to be planned.

See [foundation status](docs/FOUNDATION.md) for validation results and remaining work.
