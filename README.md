# Forge

A self-hosted, open-source AI image generator: native Android app + your own
serverless backend. No ads, no accounts required, no API key on the device.

## Architecture

```
[Android app]  --POST prompt-->  [Vercel function]  --calls-->  [Hugging Face Inference API]
   (Kotlin,                        (holds your                    (free tier, SDXL)
    Compose,                        API key)
    OkHttp)
```

## 1. Backend setup (Vercel)

1. Get a free Hugging Face account and an API token: https://huggingface.co/settings/tokens
2. `cd backend`
3. Install the Vercel CLI: `npm i -g vercel`
4. `vercel` (follow prompts to link/create a project)
5. Set your token as an env var: `vercel env add HF_API_KEY` (paste your HF token)
6. `vercel --prod` to deploy
7. Copy the deployed URL (e.g. `https://forge-backend.vercel.app`)

## 2. Android app setup

1. Open `android/` in Android Studio.
2. In `app/build.gradle.kts`, replace the placeholder in `API_BASE_URL` with your
   real Vercel URL from step 1.7 above.
3. Android Studio will generate the Gradle wrapper (`gradlew`) automatically on
   first sync — it isn't checked into this repo. If you're building via the
   GitHub Actions workflow instead of Android Studio, run `gradle wrapper`
   once locally and commit the generated `gradlew`, `gradlew.bat`, and
   `gradle/wrapper/` files so CI has something to run.
4. Build > Build APK, or run directly on a connected device/emulator.

## 3. Distribution

- **Direct install:** grab the APK from `app/build/outputs/apk/debug/` (or the
  GitHub Actions artifact) and sideload it.
- **F-Droid:** once the repo is public and the build is reproducible, you can
  submit it via an F-Droid metadata PR (https://f-droid.org/docs/Submitting_apps/).
  Kotlin/Compose with no proprietary dependencies (no Google Play Services
  used here) satisfies F-Droid's inclusion policy out of the box.

## 4. Video generation (Veo 3.1, free via Google AI Studio)

1. Get a free API key at https://aistudio.google.com/apikey (no credit card needed)
2. In Vercel: `vercel env add GEMINI_API_KEY` (paste the key)
3. Redeploy: `vercel --prod`

This adds two endpoints:
- `POST /api/generate-video` — send `{ "prompt": "..." }`, get back `{ "operationName": "..." }` immediately
- `GET /api/video-status?op=<operationName>` — poll this every ~10s; returns `{ "done": false }` while working,
  then `{ "done": true, "video": "data:video/mp4;base64,..." }` once ready (usually 1-6 minutes)

Video generation is asynchronous because it's too slow for a single request/response —
the app should call `generate-video` once, then poll `video-status` on a timer until `done` is true.

## Notes

- Swap `HF_MODEL` in Vercel's env vars to point at a different Hugging Face
  model if you want a different style/quality tradeoff than the SDXL default.
- If you'd rather run your own GPU server instead of the Hugging Face API
  (more control, no per-model rate limits), swap the `fetch` call inside
  `backend/api/generate.js` to point at your server's endpoint instead —
  the Android app doesn't need to change at all, since it only ever talks
  to your Vercel function.
