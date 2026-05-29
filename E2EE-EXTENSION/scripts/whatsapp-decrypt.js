/**
 * WhatsApp Web: show decrypted plain text under incoming cipher messages.
 */
(() => {
  if (!/web\.whatsapp\.com$/i.test(location.hostname)) return;

  const { CipherEngine, CipherDetector } = E2EE;

  let settings = {
    decryptIncoming: true,
    passphrase: CipherEngine.DEFAULT_PASSPHRASE
  };

  const processed = new WeakSet();
  let scanTimer = null;

  function loadSettings() {
    return new Promise((resolve) => {
      chrome.runtime.sendMessage({ type: "getSettings" }, (resp) => {
        if (resp) {
          settings.decryptIncoming = resp.decryptIncoming !== false;
          settings.passphrase = resp.passphrase || CipherEngine.DEFAULT_PASSPHRASE;
        }
        resolve(settings);
      });
    });
  }

  chrome.storage.onChanged.addListener((changes, area) => {
    if (area !== "sync") return;
    if (changes.decryptIncoming) settings.decryptIncoming = changes.decryptIncoming.newValue !== false;
    if (changes.passphrase) {
      settings.passphrase = changes.passphrase.newValue;
      CipherEngine.clearKeyCache();
    }
  });

  function isComposeArea(el) {
    return !!el.closest?.('footer, [contenteditable="true"][role="textbox"]');
  }

  function attachPlaintext(container, plain) {
    if (container.querySelector(".e2ee-decrypted-plain")) return;
    const box = document.createElement("div");
    box.className = "e2ee-decrypted-plain";
    box.setAttribute("data-e2ee-decrypted", "1");
    box.textContent = `🔓 ${plain}`;
    Object.assign(box.style, {
      marginTop: "4px",
      padding: "4px 8px",
      borderRadius: "6px",
      background: "rgba(16, 185, 129, 0.15)",
      border: "1px solid rgba(16, 185, 129, 0.45)",
      color: "#065f46",
      fontSize: "13px",
      fontFamily: "system-ui, sans-serif",
      lineHeight: "1.35",
      wordBreak: "break-word"
    });
    container.appendChild(box);
  }

  async function tryDecryptMessageText(el) {
    if (processed.has(el) || isComposeArea(el)) return;

    const raw = (el.textContent || "").trim();
    if (!raw || !CipherDetector.isCipherText(raw)) return;

    processed.add(el);

    const block = CipherDetector.extractExactVisualBlock(raw) || raw;
    const plain = await CipherEngine.decrypt(block, settings.passphrase);
    if (!plain) return;

    const container =
      el.closest('[data-testid="msg-container"]') ||
      el.closest('[data-id]') ||
      el.parentElement;
    if (!container) return;

    attachPlaintext(container, plain);
  }

  function scanIncomingMessages() {
    if (!settings.decryptIncoming) return;

    const main = document.querySelector("#main");
    if (!main) return;

    const candidates = main.querySelectorAll(
      'span[dir="ltr"], span[dir="rtl"], span.selectable-text.copyable-text'
    );

    for (const el of candidates) {
      if (isComposeArea(el)) continue;
      const text = (el.textContent || "").trim();
      if (text.length < 8) continue;
      if (!CipherDetector.isCipherText(text)) continue;
      tryDecryptMessageText(el);
    }
  }

  function scheduleScan() {
    clearTimeout(scanTimer);
    scanTimer = setTimeout(scanIncomingMessages, 120);
  }

  function startObserver() {
    const root = document.querySelector("#main") || document.body;
    const observer = new MutationObserver(scheduleScan);
    observer.observe(root, { childList: true, subtree: true });
    scheduleScan();
    setInterval(scanIncomingMessages, 3000);
  }

  loadSettings().then(() => {
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", startObserver);
    } else {
      startObserver();
    }
  });
})();
