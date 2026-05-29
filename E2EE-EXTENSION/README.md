# E2EE Chrome Extension

Encrypts text in **any** text box on websites when you hit **Send** (or Enter in single-line fields). Uses the same AES/CBC + visual cipher format as the **CipherKey** Android keyboard in this repo.

## Install (developer mode)

1. Open Chrome → `chrome://extensions`
2. Enable **Developer mode**
3. Click **Load unpacked**
4. Select this folder: `E2EE-EXTENSION`

## How it works

1. Type normally in any `input`, `textarea`, or `contenteditable` field.
2. Click a **Send** / **Submit** / **Post** button (or press **Enter** in a single-line input).
3. The extension replaces your plain text with encrypted cipher text, then lets the site send it.

A small **E2EE ON** badge appears at the bottom-right when encryption is enabled.

## Settings (extension popup)

- **Encrypt before send** — master on/off
- **Symbol mode** — on = `꧁…꧂` symbols; off = emoji wrappers (matches keyboard)
- **Passphrase** — must match the Android app passphrase (default: `CipherKeyDefaultSharedKey#2026!`)

## Compatibility

Messages encrypted here can be decrypted by the Android keyboard (and vice versa) when the passphrase and symbol/emoji mode match.

## WhatsApp Web

Supported: encrypts on send-button click or **Enter**. Reload the extension after updates (`chrome://extensions` → refresh).

If send still fails, open a chat, type a message, and confirm the badge shows **E2EE ON · WA**.

## Limits

- Some SPAs use custom send handlers; WhatsApp Web is explicitly supported.
- Password fields are never touched.
- Already-encrypted cipher blobs are not double-encrypted.
