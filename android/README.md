# AJM Office — Android app

A native Android app (Kotlin + Jetpack Compose, Material 3) for the two private pages of the
site. It talks to the **same Google Apps Script** as the web pages (`apps-script.gs`), so
nothing in the sheet or the script has to change.

| Web page     | In the app                                                                  |
|--------------|-----------------------------------------------------------------------------|
| `admin.html` | **Registrations** — list, stats, filters, search, sort, status, bulk actions, move to history / restore / delete, Zoom-link sends, history, CSV export, print list |
| `media.html` | **Media Office** — Content Log (day by day or whole month, Made / Uploaded), Weekly Plan, Content Stock, Stock Summary |

Like the web pages, the admin and media keys are **never saved on the phone**: you enter the key
each time the app is opened. The only thing the app remembers is the Web App URL.

## Getting the APK

Every push that touches `android/` runs the **Android app** workflow in GitHub Actions
(`.github/workflows/android.yml`). It runs the unit tests and builds a release APK.

1. Open the repository on GitHub → **Actions** → **Android app** → the latest green run.
2. Download the **AJM-Office-apk** artifact (a zip with `AJM-Office-<run>.apk` inside).
3. Copy the APK to the phone and open it (allow "install unknown apps" for your browser or Files app).

You can also start a build by hand: **Actions → Android app → Run workflow**.

Android 8.0 (API 26) or newer is needed.

## Connecting to the Google Sheet

On first launch the app asks for the **Web App URL** — the same URL as `REG_LOG_URL` in
`js/reglog.js` (Apps Script editor → Deploy → Manage deployments → Web app URL, ending in
`/exec`). It is remembered on the phone and can be changed from the login screen.

To bake the URL into every build instead, add a repository **variable** (Settings → Secrets and
variables → Actions → Variables) named `AJM_WEB_APP_URL`.

## Installing updates over the old version

Android only installs an update if it is signed with the same key as the installed app. Without
a key configured, each CI build is signed with a throwaway debug key, so you have to uninstall
the old version first (nothing is lost — the app keeps no data besides the URL).

To sign every build with one key, create a keystore once:

```sh
keytool -genkeypair -v -keystore ajm-release.jks -alias ajm -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 ajm-release.jks > ajm-release.jks.b64
```

and add these repository **secrets** (Settings → Secrets and variables → Actions → Secrets):

| Secret                  | Value                                   |
|-------------------------|-----------------------------------------|
| `AJM_KEYSTORE_BASE64`   | contents of `ajm-release.jks.b64`       |
| `AJM_KEYSTORE_PASSWORD` | the keystore password                   |
| `AJM_KEY_ALIAS`         | `ajm` (or the alias you chose)          |
| `AJM_KEY_PASSWORD`      | the key password                        |

Keep `ajm-release.jks` somewhere safe and never commit it — this repository is public.

## Building locally

With Android Studio (or JDK 17 + the Android SDK):

```sh
cd android
./gradlew :core:test :app:assembleDebug
```

## Project layout

- `core/` — plain Kotlin, no Android: the Apps Script client and the rules ported from the web
  pages (duplicates, CSV export, chart cells and merging, stock maths, the weekly plan), with unit tests.
- `app/` — the Compose UI: `ui/gate` (login), `ui/admin` (registrations), `ui/media` (media office),
  `ui/components` (shared animated building blocks), `ui/theme` (colours, Fraunces + Inter).

The weekly plan (what is made on which day, the Youth Meet date and the people doing content
selection) lives in `core/src/main/kotlin/.../media/WeeklyPlan.kt`, the same as `PLAN_ROWS` in
`media.html` — change both together.
