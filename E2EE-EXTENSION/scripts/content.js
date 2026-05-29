(() => {
  const { CipherEngine, CipherDetector } = E2EE;

  let settings = {
    enabled: true,
    useSymbols: true,
    passphrase: CipherEngine.DEFAULT_PASSPHRASE
  };

  let lastFocusedEditable = null;
  let bypassIntercept = false;
  let sendInProgress = false;

  const plainTextByField = new WeakMap();

  const SEND_BUTTON_SELECTORS = [
    '[data-testid="send"]',
    'span[data-icon="send"]',
    'button[type="submit"]',
    'input[type="submit"]',
    '[aria-label*="send" i]',
    '[title*="send" i]'
  ];

  function isWhatsApp() {
    return /web\.whatsapp\.com$/i.test(location.hostname);
  }

  function loadSettings() {
    return new Promise((resolve) => {
      chrome.storage.sync.get(
        {
          enabled: true,
          useSymbols: true,
          passphrase: CipherEngine.DEFAULT_PASSPHRASE
        },
        (stored) => {
          settings = { ...settings, ...stored };
          resolve(settings);
        }
      );
      chrome.runtime.sendMessage?.({ type: "getSettings" }, (resp) => {
        if (resp && !chrome.runtime.lastError) settings = { ...settings, ...resp };
      });
    });
  }

  chrome.storage.onChanged.addListener((changes, area) => {
    if (area !== "sync") return;
    if (changes.enabled) settings.enabled = changes.enabled.newValue;
    if (changes.useSymbols) settings.useSymbols = changes.useSymbols.newValue;
    if (changes.passphrase) {
      settings.passphrase = changes.passphrase.newValue;
      CipherEngine.clearKeyCache();
    }
    updateBadge();
  });

  function isEditable(el) {
    if (!el || el.nodeType !== Node.ELEMENT_NODE) return false;
    const tag = el.tagName;
    if (tag === "TEXTAREA") return true;
    if (tag === "INPUT") {
      const type = (el.type || "text").toLowerCase();
      return ["text", "search", "email", "url", "tel", ""].includes(type);
    }
    return !!el.isContentEditable;
  }

  function getWhatsAppComposeBox() {
    const selectors = [
      'footer div[contenteditable="true"][role="textbox"]',
      '#main footer div[contenteditable="true"]',
      'div[contenteditable="true"][data-tab="10"]',
      'div[contenteditable="true"][aria-label*="Type" i]',
      'div[contenteditable="true"][aria-label*="message" i]',
      'div[data-lexical-editor="true"][contenteditable="true"]'
    ];
    for (const sel of selectors) {
      const el = document.querySelector(sel);
      if (el) return el;
    }
    const footer = document.querySelector("footer");
    return footer?.querySelector('div[contenteditable="true"]') || null;
  }

  function getWhatsAppSendButton() {
    const list = [
      document.querySelector('footer [data-testid="send"]'),
      document.querySelector('[data-testid="send"]'),
      document.querySelector('footer span[data-icon="send"]')?.closest("button"),
      document.querySelector('span[data-icon="send"]')?.closest("button"),
      document.querySelector('footer span[data-icon="send"]')?.parentElement
    ];
    for (const el of list) {
      if (el && typeof el.click === "function") return el;
    }
    const footer = document.querySelector("footer");
    if (!footer) return null;
    const buttons = footer.querySelectorAll("button, [role='button']");
    for (const btn of buttons) {
      if (btn.querySelector?.('span[data-icon="send"]')) return btn;
      const label = (btn.getAttribute("aria-label") || "").toLowerCase();
      if (label.includes("send")) return btn;
    }
    return null;
  }

  function stripInvisible(text) {
    return text.replace(/[\u200B\u200D\u200C\u2060]/g, "");
  }

  function stripAllCipherBlocks(text) {
    let t = stripInvisible(text);
    let block = CipherDetector.extractExactVisualBlock(t);
    while (block) {
      t = t.split(block).join("");
      block = CipherDetector.extractExactVisualBlock(t);
    }
    return t.trim();
  }

  function getDomText(el) {
    if (!el) return "";
    if (el.isContentEditable) return stripInvisible(el.innerText || el.textContent || "").trim();
    return (el.value || "").trim();
  }

  function rememberPlaintext(field, text) {
    if (!field) return;
    const plain = stripAllCipherBlocks(text);
    if (plain) plainTextByField.set(field, plain);
    else plainTextByField.delete(field);
  }

  function syncPlaintextCache() {
    const field = isWhatsApp() ? getWhatsAppComposeBox() : lastFocusedEditable;
    if (field) rememberPlaintext(field, getDomText(field));
  }

  function getUserPlaintext(field) {
    if (!field) return "";
    const span = field.querySelector?.('span[data-lexical-text="true"]');
    const fromSpan = span ? stripAllCipherBlocks(span.textContent || "") : "";
    const cached = plainTextByField.get(field) || "";
    const domPlain = stripAllCipherBlocks(getDomText(field));
    return (domPlain || fromSpan || cached || "").trim();
  }

  function clearComposeField(el) {
    if (!el) return;
    el.focus();
    el.innerHTML = "";
    try {
      document.execCommand("selectAll", false);
      document.execCommand("delete", false);
    } catch {
      /* ignore */
    }
  }

  function setComposeTextOnly(el, text) {
    clearComposeField(el);
    el.focus();
    let ok = false;
    try {
      ok = document.execCommand("insertText", false, text);
    } catch {
      ok = false;
    }
    if (!ok) {
      try {
        const dt = new DataTransfer();
        dt.setData("text/plain", text);
        ok = el.dispatchEvent(
          new ClipboardEvent("paste", { bubbles: true, cancelable: true, clipboardData: dt })
        );
      } catch {
        ok = false;
      }
    }
    if (!ok) {
      const span =
        el.querySelector('span[data-lexical-text="true"]') ||
        (() => {
          const p = document.createElement("p");
          p.className = "selectable-text copyable-text";
          p.dir = "auto";
          const s = document.createElement("span");
          s.className = "selectable-text copyable-text";
          s.setAttribute("data-lexical-text", "true");
          p.appendChild(s);
          el.appendChild(p);
          return s;
        })();
      span.textContent = text;
    }
    el.dispatchEvent(
      new InputEvent("input", { bubbles: true, inputType: "insertText", data: text })
    );
  }

  function setFieldText(el, text) {
    if (!el) return;
    if (el.isContentEditable) setComposeTextOnly(el, text);
    else {
      const proto =
        el.tagName === "TEXTAREA" ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
      const setter = Object.getOwnPropertyDescriptor(proto, "value")?.set;
      el.focus();
      if (setter) setter.call(el, text);
      else el.value = text;
      el.dispatchEvent(new Event("input", { bubbles: true }));
    }
  }

  function findComposeField() {
    if (isWhatsApp()) {
      const wa = getWhatsAppComposeBox();
      if (wa) return wa;
    }
    if (lastFocusedEditable && document.contains(lastFocusedEditable)) return lastFocusedEditable;
    const active = document.activeElement;
    if (isEditable(active)) return active;
    return null;
  }

  function resolveSendClickTarget(target) {
    if (!target) return null;
    for (const sel of SEND_BUTTON_SELECTORS) {
      try {
        const hit = target.closest?.(sel) || (target.matches?.(sel) ? target : null);
        if (hit) {
          if (hit.getAttribute?.("data-icon") === "send") {
            return hit.closest("button") || hit.parentElement || hit;
          }
          return hit.closest?.("button, [role='button']") || hit;
        }
      } catch {
        /* ignore */
      }
    }
    return null;
  }

  function isWhatsAppSendInteraction(target) {
    if (!isWhatsApp()) return false;
    if (resolveSendClickTarget(target)) return true;
    const compose = getWhatsAppComposeBox();
    if (!compose || !getUserPlaintext(compose)) return false;
    const inFooter = target.closest?.("footer");
    if (!inFooter) return false;
    if (target.closest?.('span[data-icon="send"]')) return true;
    if (target.closest?.('[data-testid="send"]')) return true;
    const btn = target.closest?.("button, [role='button']");
    if (!btn) return false;
    if (btn.querySelector?.('span[data-icon="send"], [data-icon="send"]')) return true;
    const label = (btn.getAttribute("aria-label") || "").toLowerCase();
    return label.includes("send");
  }

  function resolveSendTrigger(target) {
    let sendEl = resolveSendClickTarget(target);
    if (!sendEl && isWhatsAppSendInteraction(target)) {
      sendEl = getWhatsAppSendButton() || target.closest?.("footer button, footer [role='button']");
    }
    return sendEl;
  }

  function runWithBypass(fn) {
    bypassIntercept = true;
    try {
      fn();
    } finally {
      setTimeout(() => {
        bypassIntercept = false;
      }, 800);
    }
  }

  function triggerWhatsAppSend() {
    const btn = getWhatsAppSendButton();
    if (btn) {
      runWithBypass(() => btn.click());
      return;
    }
    const compose = getWhatsAppComposeBox();
    if (!compose) return;
    runWithBypass(() => {
      compose.focus();
      compose.dispatchEvent(
        new KeyboardEvent("keydown", {
          key: "Enter",
          code: "Enter",
          keyCode: 13,
          which: 13,
          bubbles: true,
          cancelable: true
        })
      );
    });
  }

  function triggerSend(triggerEl) {
    if (isWhatsApp()) {
      triggerWhatsAppSend();
      return;
    }
    const form = triggerEl?.closest?.("form");
    if (form) {
      runWithBypass(() => {
        if (typeof form.requestSubmit === "function") form.requestSubmit();
        else form.submit();
      });
      return;
    }
    const btn = resolveSendClickTarget(triggerEl) || triggerEl;
    if (btn?.click) runWithBypass(() => btn.click());
  }

  function blockEvent(event) {
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();
  }

  async function handleSendIntent(event, triggerEl) {
    if (!settings.enabled || bypassIntercept || sendInProgress) return;

    syncPlaintextCache();
    const field = findComposeField();
    if (!field) return;

    const plain = getUserPlaintext(field);
    if (!plain || CipherDetector.isCipherText(plain)) return;

    blockEvent(event);
    sendInProgress = true;

    try {
      const encrypted = await CipherEngine.encrypt(
        plain,
        settings.useSymbols,
        settings.passphrase
      );

      if (!encrypted || encrypted === plain || !CipherDetector.isCipherText(encrypted)) {
        updateBadge("E2EE encrypt failed — sent blocked. Reload extension.");
        sendInProgress = false;
        return;
      }

      setFieldText(field, encrypted);
      plainTextByField.delete(field);

      await new Promise((r) => setTimeout(r, isWhatsApp() ? 200 : 80));
      triggerSend(triggerEl);
    } catch (err) {
      console.error("[E2EE] send encrypt error", err);
      updateBadge("E2EE error — check console");
      sendInProgress = false;
    } finally {
      setTimeout(() => {
        sendInProgress = false;
      }, 1200);
    }
  }

  function onSendInteraction(event) {
    if (!settings.enabled || bypassIntercept || sendInProgress) return;
    const sendEl = resolveSendTrigger(event.target);
    if (!sendEl && !isWhatsAppSendInteraction(event.target)) return;
    const field = findComposeField();
    if (!field || !getUserPlaintext(field)) return;
    if (CipherDetector.isCipherText(getUserPlaintext(field))) return;
    handleSendIntent(event, sendEl || event.target);
  }

  function onComposeInput(e) {
    if (!settings.enabled) return;
    const field = isWhatsApp() ? getWhatsAppComposeBox() || e.target : e.target;
    if (!isEditable(field)) return;
    rememberPlaintext(field, getDomText(field));
  }

  function onFocusIn(e) {
    if (isEditable(e.target)) lastFocusedEditable = e.target;
    syncPlaintextCache();
  }

  function onFormSubmit(e) {
    if (!settings.enabled || bypassIntercept || sendInProgress) return;
    syncPlaintextCache();
    const field = findComposeField();
    if (!field || !getUserPlaintext(field)) return;
    if (CipherDetector.isCipherText(getUserPlaintext(field))) return;
    handleSendIntent(e, e.submitter || e.target);
  }

  function onKeyDown(e) {
    if (!settings.enabled || bypassIntercept || sendInProgress || e.key !== "Enter") return;
    if (e.shiftKey || e.ctrlKey || e.metaKey || e.altKey) return;
    const field = isWhatsApp() ? getWhatsAppComposeBox() || e.target : e.target;
    if (!isEditable(field)) return;
    if (!isWhatsApp() && field.tagName === "TEXTAREA") return;
    syncPlaintextCache();
    if (!getUserPlaintext(field)) return;
    if (CipherDetector.isCipherText(getUserPlaintext(field))) return;
    handleSendIntent(e, field);
  }

  function updateBadge(text) {
    const label =
      text ||
      (settings.enabled ? (isWhatsApp() ? "E2EE ON · WA" : "E2EE ON") : "E2EE OFF");
    const bg = settings.enabled ? "rgba(16, 185, 129, 0.92)" : "rgba(239, 68, 68, 0.92)";

    let badge = document.getElementById("e2ee-ext-badge");
    if (!settings.enabled && !text) {
      badge?.remove();
      return;
    }
    if (!badge) {
      badge = document.createElement("div");
      badge.id = "e2ee-ext-badge";
      Object.assign(badge.style, {
        position: "fixed",
        bottom: "12px",
        right: "12px",
        zIndex: "2147483646",
        padding: "6px 10px",
        borderRadius: "8px",
        color: "#fff",
        font: "600 11px/1 system-ui, sans-serif",
        pointerEvents: "none",
        boxShadow: "0 2px 8px rgba(0,0,0,0.25)",
        maxWidth: "220px"
      });
      document.documentElement.appendChild(badge);
    }
    badge.textContent = label;
    badge.style.background = text?.includes("failed") || text?.includes("error") ? "rgba(239, 68, 68, 0.92)" : bg;
  }

  function init() {
    document.addEventListener("input", onComposeInput, true);
    document.addEventListener("keyup", syncPlaintextCache, true);
    document.addEventListener("focusin", onFocusIn, true);
    document.addEventListener("submit", onFormSubmit, true);
    document.addEventListener("pointerdown", onSendInteraction, true);
    document.addEventListener("mousedown", onSendInteraction, true);
    document.addEventListener("click", onSendInteraction, true);
    document.addEventListener("keydown", onKeyDown, true);

    loadSettings().then(() => updateBadge());
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
