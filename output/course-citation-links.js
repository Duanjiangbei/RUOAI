(function () {
  "use strict";

  var STORAGE_KEY = "courseCitationRecords";
  var MAX_RECORDS = 80;
  var state = {
    records: loadRecords(),
    headers: {},
    processing: false,
    timer: null
  };

  function loadRecords() {
    try {
      var raw = window.localStorage.getItem(STORAGE_KEY);
      var parsed = raw ? JSON.parse(raw) : [];
      return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
      return [];
    }
  }

  function saveRecords() {
    try {
      window.localStorage.setItem(STORAGE_KEY, JSON.stringify(state.records.slice(0, MAX_RECORDS)));
    } catch (error) {
      // Ignore storage quota or private-mode failures.
    }
  }

  function normalizeUrl(url) {
    try {
      return new URL(String(url), window.location.origin).pathname;
    } catch (error) {
      return String(url || "");
    }
  }

  function isChatSend(url) {
    return normalizeUrl(url) === "/chat/send";
  }

  function isMessageList(url) {
    return normalizeUrl(url) === "/system/message/list";
  }

  function copyUsefulHeaders(headers) {
    var result = {};
    if (!headers) {
      return result;
    }
    try {
      if (typeof headers.forEach === "function") {
        headers.forEach(function (value, key) {
          result[String(key).toLowerCase()] = value;
        });
      } else if (Array.isArray(headers)) {
        headers.forEach(function (pair) {
          if (pair && pair.length >= 2) {
            result[String(pair[0]).toLowerCase()] = pair[1];
          }
        });
      } else {
        Object.keys(headers).forEach(function (key) {
          result[String(key).toLowerCase()] = headers[key];
        });
      }
    } catch (error) {
      return result;
    }
    return result;
  }

  function parseRequestBody(body) {
    if (!body || typeof body !== "string") {
      return null;
    }
    try {
      var parsed = JSON.parse(body);
      var question = parsed.content || "";
      if (!question && Array.isArray(parsed.messages)) {
        for (var i = parsed.messages.length - 1; i >= 0; i--) {
          if (parsed.messages[i] && parsed.messages[i].content) {
            question = parsed.messages[i].content;
            break;
          }
        }
      }
      if (!question || !parsed.knowledgeId) {
        return null;
      }
      return {
        question: question,
        knowledgeId: String(parsed.knowledgeId),
        sessionId: parsed.sessionId ? String(parsed.sessionId) : getSessionIdFromPath(),
        model: parsed.model || ""
      };
    } catch (error) {
      return null;
    }
  }

  function getSessionIdFromPath() {
    var match = window.location.pathname.match(/^\/chat\/(\d+)/);
    return match ? match[1] : "";
  }

  function storeRecord(record) {
    if (!record || !Array.isArray(record.citations) || record.citations.length === 0) {
      return;
    }
    if (record.messageId) {
      state.records = state.records.filter(function (item) {
        return item.messageId !== record.messageId;
      });
    }
    record.id = [
      record.sessionId || "session",
      record.knowledgeId || "knowledge",
      Date.now(),
      Math.random().toString(16).slice(2)
    ].join("-");
    record.createdAt = Date.now();
    state.records = [record].concat(state.records).slice(0, MAX_RECORDS);
    saveRecords();
    scheduleProcess(900);
  }

  function parseCitations(value) {
    if (!value) {
      return [];
    }
    if (Array.isArray(value)) {
      return value;
    }
    try {
      var parsed = typeof value === "string" ? JSON.parse(value) : value;
      if (typeof parsed === "string") {
        return parseCitations(parsed);
      }
      if (Array.isArray(parsed)) {
        return parsed;
      }
      if (parsed && Array.isArray(parsed.data)) {
        return parsed.data;
      }
      if (parsed && Array.isArray(parsed.rows)) {
        return parsed.rows;
      }
    } catch (error) {
      return [];
    }
    return [];
  }

  function rememberHistoryCitations(payload) {
    var rows = [];
    if (payload && Array.isArray(payload.rows)) {
      rows = payload.rows;
    } else if (payload && payload.data && Array.isArray(payload.data.rows)) {
      rows = payload.data.rows;
    } else if (Array.isArray(payload)) {
      rows = payload;
    }
    if (!rows.length) {
      return;
    }
    var lastQuestion = "";
    rows.forEach(function (row) {
      if (!row) {
        return;
      }
      if (row.role === "user") {
        lastQuestion = row.content || lastQuestion;
        return;
      }
      var citations = parseCitations(row.remark);
      if (!citations.length) {
        return;
      }
      storeRecord({
        sessionId: row.sessionId ? String(row.sessionId) : getSessionIdFromPath(),
        question: lastQuestion,
        citations: citations,
        messageId: row.id ? String(row.id) : "",
        answerContent: row.content || "",
        source: "history"
      });
    });
  }

  function buildRetrievalHeaders(extraHeaders) {
    var headers = {
      "Content-Type": "application/json"
    };
    var source = Object.assign({}, state.headers, extraHeaders || {});
    ["authorization", "clientid", "client-id"].forEach(function (key) {
      if (source[key]) {
        headers[key === "clientid" ? "clientid" : key] = source[key];
      }
    });
    return headers;
  }

  function fetchCitations(request, headers) {
    if (!request || !request.question || !request.knowledgeId) {
      return;
    }
    var body = JSON.stringify({
      knowledgeId: request.knowledgeId,
      query: request.question
    });
    window.fetch("/system/fragment/retrieval", {
      method: "POST",
      credentials: "include",
      headers: buildRetrievalHeaders(headers),
      body: body
    }).then(function (response) {
      return response.json();
    }).then(function (json) {
      var citations = Array.isArray(json) ? json : json && Array.isArray(json.data) ? json.data : [];
      storeRecord({
        sessionId: request.sessionId || getSessionIdFromPath(),
        knowledgeId: request.knowledgeId,
        question: request.question,
        citations: citations
      });
    }).catch(function (error) {
      console.warn("加载引用片段失败", error);
    });
  }

  function patchFetch() {
    if (!window.fetch || window.__courseCitationFetchPatched) {
      return;
    }
    window.__courseCitationFetchPatched = true;
    var originalFetch = window.fetch;
    window.fetch = function (input, init) {
      var url = typeof input === "string" ? input : input && input.url;
      var headers = {};
      try {
        headers = copyUsefulHeaders((init && init.headers) || (input && input.headers));
        state.headers = Object.assign({}, state.headers, headers);
        if (isChatSend(url)) {
          var request = parseRequestBody(init && init.body);
          if (request) {
            setTimeout(function () {
              fetchCitations(request, headers);
            }, 100);
          }
        }
      } catch (error) {
        console.warn("引用监听 fetch 失败", error);
      }
      var responsePromise = originalFetch.apply(this, arguments);
      try {
        if (isMessageList(url)) {
          responsePromise.then(function (response) {
            if (!response || typeof response.clone !== "function") {
              return;
            }
            response.clone().json().then(rememberHistoryCitations).catch(function () {});
          }).catch(function () {});
        }
      } catch (error) {
        console.warn("引用历史监听失败", error);
      }
      return responsePromise;
    };
  }

  function patchXhr() {
    if (!window.XMLHttpRequest || window.__courseCitationXhrPatched) {
      return;
    }
    window.__courseCitationXhrPatched = true;
    var proto = window.XMLHttpRequest.prototype;
    var originalOpen = proto.open;
    var originalSetRequestHeader = proto.setRequestHeader;
    var originalSend = proto.send;

    proto.open = function (method, url) {
      this.__courseCitationMethod = method;
      this.__courseCitationUrl = url;
      this.__courseCitationHeaders = {};
      return originalOpen.apply(this, arguments);
    };

    proto.setRequestHeader = function (name, value) {
      if (this.__courseCitationHeaders) {
        this.__courseCitationHeaders[String(name).toLowerCase()] = value;
        state.headers[String(name).toLowerCase()] = value;
      }
      return originalSetRequestHeader.apply(this, arguments);
    };

    proto.send = function (body) {
      try {
        if (this.__courseCitationMethod && String(this.__courseCitationMethod).toUpperCase() === "POST" && isChatSend(this.__courseCitationUrl)) {
          var request = parseRequestBody(body);
          var headers = Object.assign({}, this.__courseCitationHeaders || {});
          if (request) {
            setTimeout(function () {
              fetchCitations(request, headers);
            }, 100);
          }
        }
        if (this.__courseCitationMethod && String(this.__courseCitationMethod).toUpperCase() === "GET" && isMessageList(this.__courseCitationUrl)) {
          this.addEventListener("load", function () {
            try {
              rememberHistoryCitations(JSON.parse(this.responseText || "{}"));
            } catch (error) {}
          });
        }
      } catch (error) {
        console.warn("引用监听 XHR 失败", error);
      }
      return originalSend.apply(this, arguments);
    };
  }

  function normalizeText(value) {
    return String(value || "").replace(/\s+/g, "");
  }

  function getContainerText(element) {
    var current = element;
    for (var i = 0; current && i < 8; i++) {
      if (current.textContent && current.textContent.length > 40) {
        return current.textContent;
      }
      current = current.parentElement;
    }
    return element && element.ownerDocument ? element.ownerDocument.body.textContent : "";
  }

  function matchScore(recordText, domText) {
    var a = normalizeText(recordText);
    var b = normalizeText(domText);
    if (!a || !b) {
      return 0;
    }
    if (a.indexOf(b) >= 0 || b.indexOf(a) >= 0) {
      return Math.min(a.length, b.length);
    }
    var windowText = b.length > 120 ? b.slice(0, 120) : b;
    var best = 0;
    for (var i = 0; i < windowText.length; i += 12) {
      var part = windowText.slice(i, i + 36);
      if (part.length >= 16 && a.indexOf(part) >= 0) {
        best = Math.max(best, part.length);
      }
    }
    return best;
  }

  function findRecord(element) {
    var sessionId = getSessionIdFromPath();
    var candidates = state.records.filter(function (record) {
      return !sessionId || !record.sessionId || record.sessionId === sessionId;
    });
    var domText = getContainerText(element);
    var best = null;
    var bestScore = 0;
    candidates.forEach(function (record) {
      var score = matchScore(record.answerContent || "", domText);
      if (score > bestScore) {
        bestScore = score;
        best = record;
      }
    });
    if (best && bestScore >= 16) {
      return best;
    }
    return candidates[0] || state.records[0] || null;
  }

  function citationByNumber(record, number) {
    if (!record || !Array.isArray(record.citations)) {
      return null;
    }
    var index = Number(number) - 1;
    if (index < 0 || index >= record.citations.length) {
      return null;
    }
    return record.citations[index];
  }

  function escapeHtml(value) {
    return String(value == null ? "" : value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function ensureModal() {
    var existing = document.getElementById("course-citation-modal");
    if (existing) {
      return existing;
    }
    var modal = document.createElement("div");
    modal.id = "course-citation-modal";
    modal.className = "course-citation-modal";
    modal.innerHTML =
      '<div class="course-citation-mask" data-citation-close="1"></div>' +
      '<section class="course-citation-dialog" role="dialog" aria-modal="true" aria-label="引用片段">' +
      '<button class="course-citation-close" type="button" data-citation-close="1">x</button>' +
      '<h3>引用片段</h3>' +
      '<div class="course-citation-body"></div>' +
      '</section>';
    document.body.appendChild(modal);
    modal.addEventListener("click", function (event) {
      if (event.target && event.target.getAttribute("data-citation-close")) {
        closeModal();
      }
    });
    document.addEventListener("keydown", function (event) {
      if (event.key === "Escape") {
        closeModal();
      }
    });
    return modal;
  }

  function openModal(number, citation, record) {
    var modal = ensureModal();
    var body = modal.querySelector(".course-citation-body");
    if (!citation) {
      body.innerHTML =
        '<p class="course-citation-empty">暂时没有找到 [' + escapeHtml(number) + '] 对应的片段。请重新提问一次，系统会重新生成可点击引用。</p>';
    } else {
      var score = citation.score == null ? "" : Number(citation.score).toFixed(4);
      body.innerHTML =
        '<div class="course-citation-meta">' +
        '<span>编号 [' + escapeHtml(number) + ']</span>' +
        (citation.sourceName ? '<span>来源：' + escapeHtml(citation.sourceName) + '</span>' : "") +
        (citation.idx != null ? '<span>片段序号：' + escapeHtml(citation.idx) + '</span>' : "") +
        (score ? '<span>相似度：' + escapeHtml(score) + '</span>' : "") +
        '</div>' +
        (record && record.question ? '<div class="course-citation-question">问题：' + escapeHtml(record.question) + '</div>' : "") +
        '<pre>' + escapeHtml(citation.content || "无片段内容") + '</pre>';
    }
    modal.classList.add("is-open");
  }

  function closeModal() {
    var modal = document.getElementById("course-citation-modal");
    if (modal) {
      modal.classList.remove("is-open");
    }
  }

  function skipNode(node) {
    var parent = node.parentElement;
    if (!parent) {
      return true;
    }
    if (!/\[\d{1,2}\]/.test(node.nodeValue || "")) {
      return true;
    }
    return !!parent.closest(
      "script,style,textarea,input,button,a,pre,code,.course-citation-modal,.course-citation-link,[contenteditable='true']"
    );
  }

  function linkifyTextNode(node) {
    if (skipNode(node)) {
      return;
    }
    var text = node.nodeValue;
    var regex = /\[(\d{1,2})\]/g;
    var match;
    var cursor = 0;
    var fragment = document.createDocumentFragment();
    var changed = false;

    while ((match = regex.exec(text)) !== null) {
      if (match.index > cursor) {
        fragment.appendChild(document.createTextNode(text.slice(cursor, match.index)));
      }
      var button = document.createElement("button");
      button.type = "button";
      button.className = "course-citation-link";
      button.textContent = match[0];
      button.setAttribute("aria-label", "查看引用片段 " + match[0]);
      button.dataset.citationNumber = match[1];
      button.addEventListener("click", function (event) {
        event.preventDefault();
        event.stopPropagation();
        var number = this.dataset.citationNumber;
        var record = findRecord(this);
        openModal(number, citationByNumber(record, number), record);
      });
      fragment.appendChild(button);
      cursor = match.index + match[0].length;
      changed = true;
    }

    if (!changed) {
      return;
    }
    if (cursor < text.length) {
      fragment.appendChild(document.createTextNode(text.slice(cursor)));
    }
    node.parentNode.replaceChild(fragment, node);
  }

  function processDocument() {
    if (state.processing) {
      return;
    }
    state.processing = true;
    try {
      var root = document.getElementById("app") || document.body;
      var walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
      var nodes = [];
      var node;
      while ((node = walker.nextNode())) {
        if (!skipNode(node)) {
          nodes.push(node);
        }
      }
      nodes.forEach(linkifyTextNode);
    } finally {
      state.processing = false;
    }
  }

  function scheduleProcess(delay) {
    window.clearTimeout(state.timer);
    state.timer = window.setTimeout(processDocument, delay == null ? 700 : delay);
  }

  function installStyles() {
    if (document.getElementById("course-citation-style")) {
      return;
    }
    var style = document.createElement("style");
    style.id = "course-citation-style";
    style.textContent =
      ".course-citation-link{display:inline-flex;align-items:center;margin:0 2px;padding:0 5px;border:1px solid #60a5fa;border-radius:6px;background:#eff6ff;color:#2563eb;font:inherit;font-weight:600;line-height:1.35;cursor:pointer;vertical-align:baseline}" +
      ".course-citation-link:hover{background:#dbeafe;border-color:#2563eb}" +
      ".course-citation-modal{position:fixed;inset:0;z-index:9999;display:none}" +
      ".course-citation-modal.is-open{display:block}" +
      ".course-citation-mask{position:absolute;inset:0;background:rgba(15,23,42,.38)}" +
      ".course-citation-dialog{position:absolute;right:32px;top:64px;width:min(680px,calc(100vw - 64px));max-height:calc(100vh - 120px);overflow:auto;background:#fff;border-radius:10px;box-shadow:0 18px 60px rgba(15,23,42,.22);padding:22px 24px;color:#111827}" +
      ".course-citation-dialog h3{margin:0 0 14px;font-size:18px;font-weight:700}" +
      ".course-citation-close{position:absolute;right:16px;top:14px;width:28px;height:28px;border:0;border-radius:50%;background:#f3f4f6;color:#4b5563;cursor:pointer;font-size:16px}" +
      ".course-citation-meta{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:12px}" +
      ".course-citation-meta span{padding:4px 8px;border-radius:999px;background:#f1f5f9;color:#334155;font-size:13px}" +
      ".course-citation-question{margin:0 0 12px;padding:10px 12px;border-left:3px solid #60a5fa;background:#f8fafc;color:#334155}" +
      ".course-citation-body pre{white-space:pre-wrap;word-break:break-word;margin:0;padding:14px;border-radius:8px;background:#f8fafc;border:1px solid #e5e7eb;font-family:inherit;line-height:1.75}" +
      ".course-citation-empty{margin:0;color:#4b5563;line-height:1.8}";
    document.head.appendChild(style);
  }

  function observe() {
    var root = document.getElementById("app") || document.body;
    var observer = new MutationObserver(function () {
      scheduleProcess(800);
    });
    observer.observe(root, { childList: true, subtree: true, characterData: true });
  }

  function init() {
    installStyles();
    patchFetch();
    patchXhr();
    observe();
    scheduleProcess(1000);
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
