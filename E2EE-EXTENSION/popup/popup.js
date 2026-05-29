const DEFAULT_PASSPHRASE = "CipherKeyDefaultSharedKey#2026!";

const enabledEl = document.getElementById("enabled");
const useSymbolsEl = document.getElementById("useSymbols");
const decryptIncomingEl = document.getElementById("decryptIncoming");
const passphraseEl = document.getElementById("passphrase");
const saveBtn = document.getElementById("save");
const statusEl = document.getElementById("status");

function showStatus(msg) {
  statusEl.textContent = msg;
  setTimeout(() => {
    if (statusEl.textContent === msg) statusEl.textContent = "";
  }, 2500);
}

chrome.storage.sync.get(
  {
    enabled: true,
    useSymbols: true,
    decryptIncoming: true,
    passphrase: DEFAULT_PASSPHRASE
  },
  (s) => {
    enabledEl.checked = s.enabled;
    useSymbolsEl.checked = s.useSymbols;
    decryptIncomingEl.checked = s.decryptIncoming !== false;
    passphraseEl.value = s.passphrase || DEFAULT_PASSPHRASE;
  }
);

saveBtn.addEventListener("click", () => {
  const payload = {
    enabled: enabledEl.checked,
    useSymbols: useSymbolsEl.checked,
    decryptIncoming: decryptIncomingEl.checked,
    passphrase: passphraseEl.value.trim() || DEFAULT_PASSPHRASE
  };
  chrome.storage.sync.set(payload, () => {
    showStatus("Saved. Reload open tabs for passphrase changes.");
  });
});
