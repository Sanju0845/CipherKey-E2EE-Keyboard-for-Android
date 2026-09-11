# CipherKey — Encrypted Keyboard for Android

[![GitHub forks](https://img.shields.io/github/forks/Sanju0845/CipherKey-E2EE-Keyboard-for-Android?style=flat-square)](https://github.com/Sanju0845/CipherKey-E2EE-Keyboard-for-Android/network/members)
[![GitHub stars](https://img.shields.io/github/stars/Sanju0845/CipherKey-E2EE-Keyboard-for-Android?style=flat-square)](https://github.com/Sanju0845/CipherKey-E2EE-Keyboard-for-Android/stargazers)
[![GitHub watchers](https://img.shields.io/github/watchers/Sanju0845/CipherKey-E2EE-Keyboard-for-Android?style=flat-square)](https://github.com/Sanju0845/CipherKey-E2EE-Keyboard-for-Android/watchers)

**CipherKey is a system-wide encrypted keyboard for Android that encrypts messages before they leave your phone, enabling end-to-end encrypted communication across any messaging app.**

***⚠️ COPYRIGHT NOTICE: This is an original personal project by Sanju0845 (AKA Sanjay Anand Soma).***
***You must provide credit to the original author in ALL cases - personal, educational, or commercial use.***
***The author is NOT responsible for any damages, data loss, security breaches, or misuse of this code.***
***You CANNOT sue the author directly or indirectly for any issues arising from using this project.***
***By using this code, you accept full responsibility for your implementation and any consequences.***

---

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

**Cloud build (no local Gradle):** Every push to `main` runs [GitHub Actions](https://github.com/Sanju0845/CipherKey-E2EE-Keyboard-for-Android/actions). Open the latest workflow run → **Artifacts** → download the APK.

**Local build:**

```bash
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

Release APK (unsigned in CI unless you add signing secrets):

```bash
./gradlew assembleRelease
```

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

This uses a **shared password**, not per-contact Signal-style keys. Anyone with the passphrase and CipherKey can decrypt. The visual wrapper hides meaning from casual readers; use a strong custom passphrase and rotate it periodically.

## Roadmap

- Natural-looking cover text (e.g. Telugu/English chat style instead of symbols)
- Smoother typing and personal word learning
- Per-contact keys

## Related

Browser companion (optional): see [`E2EE-EXTENSION/`](E2EE-EXTENSION/) for Chrome — same cipher format, different platform.

## License

**Dual License:**
- **MIT License** - Use, modify, and distribute freely (see LICENSE-MIT)
- **CC-BY 4.0 (Creative Commons Attribution)** - Any use (including commercial) requires attribution to Sanjay Anand Soma (Sanju0845)

**Full Text:**
```
This project is provided "AS-IS" without any warranty or liability.
The author (Sanju0845/Sanjay Anand Soma) is NOT responsible for:
- Any damages, data loss, or security breaches
- Misuse of this code for malicious purposes
- Any consequences arising from using this project

You may NOT sue the author directly or indirectly for any issues.
By downloading/using this code, you accept full responsibility.
```

---

**Made with ❤️ by Sanju0845**
