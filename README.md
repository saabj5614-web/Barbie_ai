# Barbie AI

Premium Android personal assistant foundation for voice-first control, Roman Urdu-first conversation, phone actions, notifications, files and GitHub automation.

## Implemented now
- Android voice assistant UI
- Voice input and speech output
- YouTube search/open action
- WhatsApp share/open action
- Phone call flow with runtime permission
- Android file picker
- User-started foreground wake listener for **Barbie Barbie**
- Notification listener for WhatsApp/Telegram/SMS-style notifications
- Node backend with OpenRouter free-router primary and Hugging Face fallback
- Action detection endpoint
- Secure server-side GitHub file writer
- Automatic debug APK GitHub Actions workflow
- Hindi/Devanagari intentionally disabled in the AI system prompt

## API setup later
No private API key is committed. Add secrets only to the backend deployment environment. The current AI adapters use OpenRouter's `openrouter/free` router and Hugging Face Inference Providers as fallback. Provider limits and free availability can change.

## Android limitations
Android controls background microphone, notification access, calls and WhatsApp automation. Barbie requests permissions only where needed. A normal Android app cannot silently send WhatsApp messages or freely answer/reject calls without the appropriate Android role/permission.

## Project boundary
This is the new `Barbie_ai` repository. The older `Barbie_mini_bot` repository is not used or modified here.
