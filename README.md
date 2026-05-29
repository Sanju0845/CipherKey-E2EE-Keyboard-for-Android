# CipherKey — Encrypted Keyboard for Android

CipherKey is a custom Android keyboard that encrypts what you type **before** it goes into WhatsApp, Telegram, Instagram, SMS, or any other app. Friends who use CipherKey with the **same passphrase** can read your real message; everyone else only sees encoded text.

## Why this exists

Most secret-chat apps only work inside one messenger. CipherKey works **across apps** because encryption happens at the **keyboard layer** — you keep using the apps you already have.

## How it works

1. Install CipherKey and enable it as a system keyboard.
2. Turn **Lock ON** (cipher mode) and type your message.
3. On **space** or **enter**, plaintext is encrypted on your phone and only the ciphertext is sent to the chat.
4. To read a friend's message: turn **Lock OFF**, copy the encrypted bubble, open CipherKey, tap **Read** — the toolbar shows the decrypted text.

**Crypto:** AES-128 (CBC) with a shared passphrase (SHA-256 key derivation). Ciphertext is wrapped in a custom visual format (symbols or emojis) so it does not look like normal typing.

## Features

- System-wide custom IME (Input Method Editor)
- Cipher mode for sending encrypted messages
- Decrypt preview in the keyboard toolbar
- Shared group passphrase (default key or custom secret)
- Setup app: enable keyboard, set passphrase, live encrypt/decrypt playground
- Symbol and emoji visual encoding modes

## Requirements

- Android 7.0+ (API 24+)
- [Android Studio](https://developer.android.com/studio) (to build from source)

## Build from source

```bash
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Setup on your phone

1. Install the APK.
2. Open **CipherKey** and follow the steps to **enable** the keyboard and **set it as active**.
3. Set a **Group Encryption Password** if you want a private key (both sides must use the same passphrase).
4. In any app, switch to the CipherKey keyboard and use Lock ON/OFF as needed.

## Project layout

| Path | Purpose |
|------|---------|
| `app/src/main/java/com/example/keyboard/` | Keyboard UI and IME service |
| `app/src/main/java/com/example/cipher/` | Encryption, detection, visual encoding |
| `app/src/main/java/com/example/MainActivity.kt` | Onboarding and crypto playground |

## Security note

This uses a **shared password**, not per-contact Signal-style keys. Anyone with the passphrase and CipherKey can decrypt. The visual wrapper hides meaning from casual readers; use a strong custom passphrase for better privacy.

## Roadmap

- Natural-looking cover text (e.g. Telugu/English chat style instead of symbols)
- Smoother typing and personal word learning
- Per-contact keys

## Related

Browser companion (optional): see [`E2EE-EXTENSION/`](E2EE-EXTENSION/) for Chrome — same cipher format, different platform.

## License

MIT (or specify your license here).
