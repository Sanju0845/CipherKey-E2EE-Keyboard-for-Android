const DEFAULTS = {
  enabled: true,
  useSymbols: true,
  decryptIncoming: true,
  passphrase: "CipherKeyDefaultSharedKey#2026!"
};

chrome.runtime.onInstalled.addListener(() => {
  chrome.storage.sync.get(DEFAULTS, (stored) => {
    chrome.storage.sync.set({ ...DEFAULTS, ...stored });
  });
});

chrome.runtime.onMessage.addListener((msg, _sender, sendResponse) => {
  if (msg.type === "getSettings") {
    chrome.storage.sync.get(DEFAULTS, (settings) => sendResponse(settings));
    return true;
  }
  if (msg.type === "setSettings") {
    chrome.storage.sync.set(msg.payload, () => sendResponse({ ok: true }));
    return true;
  }
});
