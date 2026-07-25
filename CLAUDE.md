# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

BuyerApp — the client-side Android app of a two-sided freelance marketplace (buyers hire freelancers). Native Android, single `:app` module, written in Kotlin with XML views (no Compose) and view binding.

## Build & run

Windows shell (this repo lives on `D:\`), use the Gradle wrapper:

```
.\gradlew.bat assembleDebug          # build debug APK
.\gradlew.bat installDebug           # build + install on connected device/emulator
.\gradlew.bat test                   # JVM unit tests (app/src/test)
.\gradlew.bat testDebugUnitTest --tests "com.nikhil.buyerapp.SomeTest"   # single unit test
.\gradlew.bat connectedAndroidTest   # instrumented tests (app/src/androidTest), needs a device/emulator
.\gradlew.bat lint                   # Android Lint
```

There are effectively no real tests yet — `app/src/test` and `app/src/androidTest` only contain the default `ExampleUnitTest` / `ExampleInstrumentedTest` stubs.

### Required local config

`app/build.gradle.kts` reads secrets from a git-ignored `local.properties` at the repo root and injects them as `BuildConfig` fields. Without these keys present, the debug/release build will still compile but the fields will be empty strings and network calls (Gemini, Supabase, News) will fail at runtime:

- `SUPABASE_URL`, `SUPABASE_KEY` — Supabase (storage) client, initialized in `comprofile/supabasefile.kt` (the `Application` class)
- `GOOGLE_KEY` — Gemini API key, used by `utils/GeminiClient.kt`
- `NEWS_KEY` — newsdata.io API key, used by the news feature
- `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` — release signing config

`app/google-services.json` (Firebase) is checked into the repo.

## Architecture

### Navigation model: two-stage flow, then single-Activity bottom-nav shell

- `MainActivity` is the launcher activity. It plays a splash Lottie animation, then checks Firebase Auth (`auth.currentUser`) and the `Users/{uid}` Firestore document (`profilecomplete`, `approved` fields) to route to one of three destinations: `Signup.SignupActivity` (no/incomplete profile), `comprofile.EnterCode` (profile done, OTP/approval pending), or `basichome.hosthome` (fully onboarded).
- `basichome.hosthome` is the main shell Activity: a `NavHostFragment` + `BottomNavigationView` driven by `res/navigation/nav_graph.xml`. All main-app screens (home, search, orders, chat, profile, gemini AI, freelancer search, reviews, edit profile, add-project) are fragments inside this one graph/Activity, not separate Activities. The bottom nav auto-hides on non-top-level destinations (see `hosthome.showBottomNav`/`hideBottomNav`).
- Feature-specific standalone Activities exist outside the shell for flows that shouldn't show bottom nav: `Login/LoginActivity`, `Signup/SignupActivity` + `SignupActivity2`, `comprofile/EnterCode`, `comprofile/ProfileScreen1` + `ProfileScreen2`.
- First-run UX: `hosthome` shows a sequential `TapTargetView` coach-mark tour over the bottom nav items, gated per-item by flags in the `"hints"` SharedPreferences (`home_hint_shows`, `chat_hint_shows`, `order_hint_shows`, `ai_hint_shown`, `freelancer_full_profile_hint_shown`).

### Package layout (by feature, not by layer)

- `basichome/` — the bottom-nav fragments (Home, Search, Chat, Order, Profile) plus `hosthome` shell and the Gemini AI fragment.
- `Login/`, `Signup/` — auth and onboarding screens (note: capitalized package names, inconsistent with the rest — keep matching existing casing when adding files to these packages).
- `comprofile/` — company/client profile setup (`ProfileScreen1`/`2`, `EnterCode`) and `supabasefile` (the `Application` subclass wiring Firebase Firestore settings + Supabase client — this is where any new global client/service init belongs).
- `chatting/` — the 1:1 chat UI (`ChatInterface`, `ChatAdapter`) backed by Firestore `Chat`/`messages`.
- `freelanceprofileview/` — read-only view of a freelancer's public profile (experience, skills, qualifications, certifications, reviews) shown to buyers.
- `freelancesearch/` — freelancer search/discovery.
- `Orders/`, `displayingorders/`, `OrdersList.kt` — project/order creation and listing (Firestore `Projects`).
- `profile/` — the buyer's own profile (`CoreProfileFragment`, `EditProfileFragment`).
- `news/`, `mailretro/` — Retrofit clients for external APIs (newsdata.io; a custom mail/skill-matching backend at `skill-vedikaser.vercel.app`).
- `dataclasses/` — all Firestore/Retrofit model POJOs (`User`, `Client`, `Freelancer`, `Project`, `Chat`, `Message`, `Review`, etc.).
- `skills/` — skill category/data models used in search and profile editing.
- `utils/` — small shared helpers: `Extensions.kt` (activity/fragment extension functions — `loge`/`logd` auto-tagged logging, `snack()`, `Navigateto`/`Navigatetoclear`), `GeminiClient.kt` (resume-vs-job-description analysis via `gemini-2.5-flash`), `UserUtils.kt` (fetch current user's display name with Auth→Firestore fallback).

### Backend services (no custom backend server for core data)

- **Firebase Firestore** is the primary datastore. Known top-level collections: `Users`, `Client`, `Freelancers`, `Projects`, `Skills`, `Chat`, `messages`. Persistence is enabled app-wide (offline cache) in `supabasefile.onCreate`.
- **Firebase Auth** for authentication.
- **Supabase Storage** (not Postgres/Auth) for file/image storage — client created in `supabasefile` via `createSupabaseClient` with only the `Storage` plugin installed.
- **Retrofit + Gson** for two unrelated external HTTP APIs, each with its own `object` singleton client (`news/RetroNews`, `mailretro/Retromail`) — follow this same per-API singleton pattern if adding another REST integration, rather than sharing a single Retrofit instance.
- **Google Generative AI SDK** (Gemini) direct from the client for resume/job-description matching (`utils/GeminiClient.kt`).

### Dependency version constraints (do not casually bump)

`app/build.gradle.kts` pins Ktor to 2.3.12 and Supabase BOM to 2.6.1 specifically because Supabase 3.0+ forces Ktor 3, which conflicts with the `generativeai` (Gemini) SDK's Ktor 2 dependency. If touching Supabase, Ktor, or the Gemini SDK, keep this compatibility constraint in mind (see comments in `app/build.gradle.kts`).

Most first-party AndroidX/Firebase/Google versions are centralized in `gradle/libs.versions.toml` (`libs.*` catalog), but a number of third-party libs (Glide, Retrofit, Ktor, Supabase, Lottie, shimmer, PDFBox, Markwon, taptargetview, CCP, androidsvg) are declared as raw version strings directly in `app/build.gradle.kts` instead of the catalog — that split is pre-existing, not a mistake to "fix" incidentally.

### Build outputs are gitignored

`app/release/` (APK, AAB, baseline profile `.dm` files, `output-metadata.json`) was previously committed to git by accident (missing `.gitignore` entries), then untracked and gitignored. Do not commit files from `app/release/` going forward — if `git status` shows them as modified/untracked again, that means `.gitignore` regressed, not that they need to be re-added.