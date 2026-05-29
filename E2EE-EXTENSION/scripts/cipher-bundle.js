/**
 * CipherKey crypto — ported from Android app for Chrome extension compatibility.
 */
const E2EE = (() => {
  const DEFAULT_PASSPHRASE = "CipherKeyDefaultSharedKey#2026!";

  const SYMBOLS = [
    "⟟", "☍", "∆", "☄", "༒", "⌘", "⇢", "⊕", "⌿", "⍓", "⚙", "✦", "☯", "🔱", "❖", "⧓"
  ];
  const EMOJIS = [
    "🌙", "✨", "☄️", "🫧", "⚡", "🪐", "🔮", "🧿", "🧬", "🖤", "🌌", "🚀", "👾", "📡", "🛡️", "🔑"
  ];

  const SYMBOL_PREFIX = "꧁";
  const SYMBOL_SUFFIX = "꧂";
  const EMOJI_PREFIX = "🌌✨";
  const EMOJI_SUFFIX = "✨🌌";

  const INVISIBLE_CHARS = new Set(["\u200B", "\u200D", "\u200C", "\u2060"]);

  let cachedKey = null;
  let cachedPassphrase = null;

  function bytesToHex(bytes) {
    const lookup = "0123456789abcdef";
    let out = "";
    for (let i = 0; i < bytes.length; i++) {
      const v = bytes[i] & 0xff;
      out += lookup[v >> 4] + lookup[v & 0x0f];
    }
    return out;
  }

  function hexToBytes(hex) {
    if (hex.length % 2 !== 0) return null;
    const bytes = new Uint8Array(hex.length / 2);
    for (let i = 0; i < bytes.length; i++) {
      const d1 = parseInt(hex[i * 2], 16);
      const d2 = parseInt(hex[i * 2 + 1], 16);
      if (Number.isNaN(d1) || Number.isNaN(d2)) return null;
      bytes[i] = (d1 << 4) + d2;
    }
    return bytes;
  }

  async function deriveKey(passphrase) {
    if (cachedKey && cachedPassphrase === passphrase) return cachedKey;
    try {
      const hash = await crypto.subtle.digest(
        "SHA-256",
        new TextEncoder().encode(passphrase)
      );
      cachedKey = await crypto.subtle.importKey(
        "raw",
        hash.slice(0, 16),
        { name: "AES-CBC" },
        false,
        ["encrypt", "decrypt"]
      );
      cachedPassphrase = passphrase;
      return cachedKey;
    } catch {
      const padded = passphrase.padEnd(16, "0").slice(0, 16);
      cachedKey = await crypto.subtle.importKey(
        "raw",
        new TextEncoder().encode(padded),
        { name: "AES-CBC" },
        false,
        ["encrypt", "decrypt"]
      );
      cachedPassphrase = passphrase;
      return cachedKey;
    }
  }

  function clearKeyCache() {
    cachedKey = null;
    cachedPassphrase = null;
  }

  const InvisibleCharInjector = {
    inject(text) {
      const pool = ["\u200B", "\u200D", "\u200C", "\u2060"];
      let result = "";
      for (const char of text) {
        result += char;
        if (Math.random() < 0.3) {
          result += pool[Math.floor(Math.random() * pool.length)];
        }
      }
      return result;
    },
    remove(text) {
      return [...text].filter((c) => !INVISIBLE_CHARS.has(c)).join("");
    }
  };

  const SymbolMapper = {
    detectMode(cleanedText) {
      const symbolCount = SYMBOLS.filter((s) => cleanedText.includes(s)).length;
      const emojiCount = EMOJIS.filter((e) => cleanedText.includes(e)).length;
      return symbolCount >= emojiCount;
    },
    mapToVisual(hexString, useSymbols, rotation) {
      const pool = useSymbols ? SYMBOLS : EMOJIS;
      const r = rotation % 16;
      const rotated = Array.from({ length: 16 }, (_, i) => pool[(i + r) % 16]);
      let result = "";
      for (const char of hexString) {
        const digit = parseInt(char, 16);
        if (digit >= 0 && digit <= 15) result += rotated[digit];
      }
      return result;
    },
    mapFromVisual(visualString, useSymbols, rotation) {
      const pool = useSymbols ? SYMBOLS : EMOJIS;
      const r = rotation % 16;
      const rotated = Array.from({ length: 16 }, (_, i) => pool[(i + r) % 16]);
      let result = "";
      let i = 0;
      while (i < visualString.length) {
        let matched = false;
        for (let digit = 0; digit < 16; digit++) {
          const symbol = rotated[digit];
          if (visualString.startsWith(symbol, i)) {
            result += digit.toString(16);
            i += symbol.length;
            matched = true;
            break;
          }
        }
        if (!matched) i++;
      }
      return result;
    }
  };

  const UnicodeObfuscator = {
    SYMBOL_PREFIX,
    SYMBOL_SUFFIX,
    EMOJI_PREFIX,
    EMOJI_SUFFIX,
    obfuscate(hexString, useSymbols) {
      const rotation = Math.floor(Math.random() * 16);
      const rotationHex = rotation.toString(16);
      const prefixCharVisual = SymbolMapper.mapToVisual(rotationHex, useSymbols, 0);
      const bodyVisual = SymbolMapper.mapToVisual(hexString, useSymbols, rotation);
      const rawVisual = useSymbols
        ? SYMBOL_PREFIX + prefixCharVisual + bodyVisual + SYMBOL_SUFFIX
        : EMOJI_PREFIX + prefixCharVisual + bodyVisual + EMOJI_SUFFIX;
      return InvisibleCharInjector.inject(rawVisual);
    },
    deobfuscate(obfuscatedText) {
      const cleaned = InvisibleCharInjector.remove(obfuscatedText).trim();
      const useSymbols = SymbolMapper.detectMode(cleaned);
      let stripped;
      if (useSymbols) {
        if (!cleaned.startsWith(SYMBOL_PREFIX) || !cleaned.endsWith(SYMBOL_SUFFIX)) return null;
        stripped = cleaned.slice(SYMBOL_PREFIX.length, cleaned.length - SYMBOL_SUFFIX.length);
      } else {
        if (!cleaned.startsWith(EMOJI_PREFIX) || !cleaned.endsWith(EMOJI_SUFFIX)) return null;
        stripped = cleaned.slice(EMOJI_PREFIX.length, cleaned.length - EMOJI_SUFFIX.length);
      }
      if (!stripped) return null;
      const pool = useSymbols ? SYMBOLS : EMOJIS;
      let rotationDigit = -1;
      let rotationHexSymbol = "";
      for (let digit = 0; digit < 16; digit++) {
        const symbol = pool[digit];
        if (stripped.startsWith(symbol)) {
          rotationHexSymbol = symbol;
          rotationDigit = digit;
          break;
        }
      }
      if (rotationDigit === -1) return null;
      const bodyVisual = stripped.slice(rotationHexSymbol.length);
      return SymbolMapper.mapFromVisual(bodyVisual, useSymbols, rotationDigit);
    }
  };

  const CipherDetector = {
    isCipherText(text) {
      const cleaned = InvisibleCharInjector.remove(text).trim();
      return (
        (cleaned.includes(SYMBOL_PREFIX) && cleaned.includes(SYMBOL_SUFFIX)) ||
        (cleaned.includes(EMOJI_PREFIX) && cleaned.includes(EMOJI_SUFFIX))
      );
    },
    extractExactVisualBlock(text) {
      const symStart = text.indexOf(SYMBOL_PREFIX);
      if (symStart !== -1) {
        const symEnd = text.indexOf(SYMBOL_SUFFIX, symStart + SYMBOL_PREFIX.length);
        if (symEnd !== -1) return text.slice(symStart, symEnd + SYMBOL_SUFFIX.length);
      }
      const emoStart = text.indexOf(EMOJI_PREFIX);
      if (emoStart !== -1) {
        const emoEnd = text.indexOf(EMOJI_SUFFIX, emoStart + EMOJI_PREFIX.length);
        if (emoEnd !== -1) return text.slice(emoStart, emoEnd + EMOJI_SUFFIX.length);
      }
      return null;
    }
  };

  const CipherEngine = {
    DEFAULT_PASSPHRASE,
    clearKeyCache,
    async encrypt(plainText, useSymbols, passphrase = DEFAULT_PASSPHRASE) {
      if (!plainText) return "";
      try {
        const key = await deriveKey(passphrase || DEFAULT_PASSPHRASE);
        const iv = crypto.getRandomValues(new Uint8Array(16));
        const encoded = new TextEncoder().encode(plainText);
        const ciphertext = await crypto.subtle.encrypt({ name: "AES-CBC", iv }, key, encoded);
        const combined = new Uint8Array(iv.length + ciphertext.byteLength);
        combined.set(iv);
        combined.set(new Uint8Array(ciphertext), iv.length);
        const hexString = bytesToHex(combined);
        return UnicodeObfuscator.obfuscate(hexString, useSymbols);
      } catch (e) {
        console.error("[E2EE] encrypt failed", e);
        return plainText;
      }
    },
    async decrypt(encryptedVisualText, passphrase = DEFAULT_PASSPHRASE) {
      try {
        const block =
          CipherDetector.extractExactVisualBlock(encryptedVisualText) || encryptedVisualText;
        const hexString = UnicodeObfuscator.deobfuscate(block);
        if (!hexString) return null;
        const combinedBytes = hexToBytes(hexString);
        if (!combinedBytes || combinedBytes.length <= 16) return null;
        const iv = combinedBytes.slice(0, 16);
        const encryptedBytes = combinedBytes.slice(16);
        const key = await deriveKey(passphrase || DEFAULT_PASSPHRASE);
        const decrypted = await crypto.subtle.decrypt(
          { name: "AES-CBC", iv },
          key,
          encryptedBytes
        );
        return new TextDecoder().decode(decrypted);
      } catch (e) {
        console.error("[E2EE] decrypt failed", e);
        return null;
      }
    }
  };

  return { CipherEngine, CipherDetector, InvisibleCharInjector };
})();
