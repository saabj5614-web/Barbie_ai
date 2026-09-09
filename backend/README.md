# Barbie AI Backend

Node 18+ backend for Barbie AI. It keeps provider secrets off the Android APK and supports provider fallback.

## Providers
1. OpenRouter: uses `OPENROUTER_API_KEY` and the `openrouter/free` router.
2. Hugging Face Inference Providers: uses `HUGGINGFACE_TOKEN` as fallback.

No API key is committed to GitHub. Free quotas and provider availability can change, so the project does not promise unlimited lifetime cloud usage.

## Run
`node server.js`

Health: `GET /health`
Chat: `POST /api/chat` with `{ "message": "..." }`
