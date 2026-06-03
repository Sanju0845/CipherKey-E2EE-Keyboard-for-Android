# CipherKey — Features & Architecture

> Latest: Version 1.2.2

---

## What is CipherKey?

CipherKey is a fully custom Android keyboard (IME) — a drop-in replacement for Gboard or SwiftKey — with **end-to-end AES-128 encryption built directly into the typing experience**.

Messages are encrypted *before* they leave your keyboard, meaning no app, server, or network in between can read them. The recipient needs the same CipherKey app with the same passphrase to decrypt.

---

## Changelog

### v1.2.2 — Cover Profiles (Disguise Messages)
- Encrypted messages can now be disguised as normal-looking text
- **5 cover profiles available:**
  - ✦ **Symbols** — default cipher symbols (unchanged)
  - 🌙 **Emojis** — emoji cipher mode (unchanged)
  - 🏏 **Cricket** — `IND 186/4 Ov 18.3 RR 10.1 Target 212`
  - 🛒 **Shopping** — `Milk x2 Rice x1 Sugar x3 Tea x1`
  - 📝 **Notes** — `Project Ideas - Camera Module - Sensor Testing`
  - 🎬 **Movie Review** — `Movie: Horizon Story: 7/10 Music: 8/10`
  - 💻 **Tech Log** — `INFO 14:22:31 Connection Stable Response 200`
- **How to use:** Long press the 🔒 cipher toggle in the bottom row → profile picker slides in → select a profile → all future encrypted messages use that cover
- Payload is hidden in the cover text using invisible unicode markers
- CipherKey automatically detects and extracts cover-encoded messages on decrypt
- Fully compatible with HMAC integrity verification

### v1.2.1 — Quick Decrypt Bubble (Text Selection Actions)
- **🔒 Encrypt** and **🔓 Decrypt** buttons now appear directly in Android's text selection toolbar
- Works in **any app** — WhatsApp, Telegram, Notes, Gmail, Chrome, Instagram, anywhere text can be selected
- **How to use:**
  - Long press any text → select it → tap **🔒 Encrypt** → selected text is replaced with cipher symbols
  - Long press any cipher message → select it → tap **🔓 Decrypt** → replaced with plaintext
- Shows toast: `🔒 Encrypted`, `🔓 Decrypted`, or `⚠ Integrity warning` depending on result
- Decrypt also respects HMAC integrity — warns if message was tampered
- Zero UI — completely invisible Activity, no screen shown, just instant in-place replacement

### v1.2.0 — AES + HMAC-SHA256 Message Integrity
  - Every encrypted message now includes an 8-byte HMAC-SHA256 tag
  - On decrypt, HMAC is verified before returning plaintext
  - Clipboard decrypt preview shows `✓ Verified` (green) if intact or `⚠ Integrity Failed` (red) if tampered
  - `Paste into field` button is hidden when integrity fails — can't paste corrupted data
  - Fully backward-compatible with v1.1.0 messages (legacy messages treated as verified)
  - Wire format: `[IV 16B] + [AES ciphertext] + [HMAC tag 8B]` → hex → visual symbols

### v1.1.0
- Smart shift (one-shot + caps lock)
- Long press top row for numbers
- Inline decrypt preview in clipboard (no auto-paste)
- Swipe-to-delete in clipboard with animated red background
- Clipboard auto-updates on system copy events
- Clipboard history (up to 20 entries)
- Word-by-word backspace on long press
- Selected text deletion on backspace
- Dot key in bottom row
- SVG/unicode icon toggle for clipboard/keyboard switch
- Removed duplicate clipboard header
- Fixed transition jitter between keyboard and clipboard panel
- Full sentence decryption (multiple cipher blocks)
- Crossfade animation between keyboard and clipboard

### v1.0.0
- Initial release: QWERTY keyboard with AES-128 encryption
- Symbol and emoji cipher modes
- Glide typing
- Word predictions
- Clipboard panel
- Onboarding screen with playground

---

## UI Layout

```
┌─────────────────────────────────────────────┐
│  word1  │  word2  │  word3          [ ❑/⌨ ] │  ← Suggestion Strip
├─────────────────────────────────────────────┤
│  q  w  e  r  t  y  u  i  o  p              │  ← Row 1 (long-press = 1–0)
│    a  s  d  f  g  h  j  k  l               │  ← Row 2
│  ⇧  z  x  c  v  b  n  m  ⌫               │  ← Row 3
│  ?123  🔒/🔓  [    space    ]  .  ↩        │  ← Bottom row
└─────────────────────────────────────────────┘
```

---

## Feature List

### ⌨️ Keyboard Core

- **Full QWERTY layout** with lowercase, uppercase, numbers, symbols, and alt-symbols pages
- **Glide / swipe typing** — slide across letter keys to form words without lifting finger
- **Smart shift key**
  - Single tap → one-shot caps (next letter only)
  - Double tap fast → caps lock (⇪, stays on until tapped again)
- **Long press top row** → types the corresponding number (q=1, w=2, … p=0), with small hint shown in corner of each key
- **Backspace**
  - Single tap → delete one character
  - Tap on selected text → delete entire selection
  - Long press → delete word by word (every 150ms)
- **Dot button** between space and enter for quick punctuation
- **Haptic feedback** on every key press

---

### 🔒 Encryption (Cipher Mode)

- Toggle `🔒 ON / 🔓 OFF` in the bottom row
- While ON, you type plaintext — the suggestion strip previews your draft
- On **Space** or **Enter** each word/sentence is AES encrypted and inserted as visual symbols or emojis
- **AES-128/CBC** with random IV per message
- Key derived from passphrase using SHA-256 → truncated to 128-bit
- Visual output options:
  - **Symbol mode**: `⟟ ☍ ∆ ☄ ༒ ⌘ ⇢ ⊕ ⌿ ⍓ ⚙ ✦ ☯ 🔱 ❖ ⧓`
  - **Emoji mode**: `🌙 ✨ ☄️ 🫧 ⚡ 🪐 🔮 🧿 🧬 🖤 🌌 🚀 👾 📡 🛡️ 🔑`
- Dynamic rotation shift embedded in each message (defeats frequency analysis)
- Zero-width invisible unicode characters sprinkled in to disrupt pattern matching

---

### 📋 Clipboard Panel

- Tap the **❑** icon (top-right of strip) to open clipboard; icon switches to **⌨** to go back
- **Auto-updating history** — listens to system clipboard changes in real time, stores up to 20 entries newest-first, deduplicates automatically
- Each card shows:
  - Plain text → tap body to paste, tap **🔒** to encrypt and paste
  - Cipher text → tap body to paste raw, tap **🔓** to preview decrypted text **inline** (no auto-paste)
- **Decrypt preview** expands below the card with:
  - `Copy` — copies decrypted text to clipboard
  - `Paste into field` — inserts decrypted text into the active text box
- **Swipe left to delete** — reveals red `🗑 Delete` background with animation; swipe 40%+ commits delete with slide-out; release early snaps back
- Panel height matches keyboard height exactly — zero layout jump on transition
- Smooth crossfade animation between keyboard and clipboard

---

### 📝 Word Predictions

- 3-word suggestion strip shown above the keys when cipher mode is OFF
- Offline, fully local — no network, no permissions
- **Bigram model** — context-aware suggestions based on the previous word (e.g. "good" → "morning / evening / night")
- **Prefix matching** — filters 300+ common English words as you type
- Capitalization-aware
- Center suggestion is visually highlighted
- Tap a suggestion → autocompletes and adds a space

---

### 🔑 Passphrase & Key Management

- Default shared passphrase so fresh installs can communicate out of the box
- Custom passphrase in the main app creates a **private subgroup**
- Key is cached after first derivation (sub-millisecond on repeat use)
- Stored in `SharedPreferences` locally

---

### 📱 Main App (Onboarding)

1. **Activate Keyboard** — opens system Input Method Settings
2. **Set as Active** — shows system keyboard picker
3. **Group Encryption Password** — set a custom passphrase
4. **Live Crypto Playground** — test encrypt/decrypt before switching keyboards

---

### 🏗️ Architecture

| Layer | Technology |
|---|---|
| UI | Jetpack Compose |
| Keyboard service | `InputMethodService` with full Compose lifecycle wiring |
| Encryption | `javax.crypto` AES/CBC/PKCS5Padding |
| Clipboard | `ClipboardManager.OnPrimaryClipChangedListener` |
| State | Compose `mutableStateOf` inside the IME service |
| Storage | `SharedPreferences` |
| Language | Kotlin |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

---

## Changelog

### v1.1.0
- Smart shift (one-shot + caps lock)
- Long press top row for numbers
- Inline decrypt preview in clipboard (no auto-paste)
- Swipe-to-delete in clipboard with animated red background
- Clipboard auto-updates on system copy events
- Clipboard history (up to 20 entries)
- Word-by-word backspace on long press
- Selected text deletion on backspace
- Dot key in bottom row
- SVG/unicode icon toggle for clipboard/keyboard switch
- Removed duplicate clipboard header
- Fixed transition jitter between keyboard and clipboard panel
- Full sentence decryption (multiple cipher blocks)
- Crossfade animation between keyboard and clipboard

### v1.0.0
- Initial release: QWERTY keyboard with AES-128 encryption
- Symbol and emoji cipher modes
- Glide typing
- Word predictions
- Clipboard panel
- Onboarding screen with playground
