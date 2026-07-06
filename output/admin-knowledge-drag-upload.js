(function () {
  var DROP_ID = "course-knowledge-drag-upload";
  var STYLE_ID = "course-knowledge-drag-upload-style";

  function ensureStyle() {
    if (document.getElementById(STYLE_ID)) {
      return;
    }
    var style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = [
      "#" + DROP_ID + " {",
      "  border: 1px dashed #1677ff;",
      "  border-radius: 8px;",
      "  background: #f7fbff;",
      "  color: #1f2937;",
      "  cursor: pointer;",
      "  margin: 12px 0 16px;",
      "  padding: 18px 16px;",
      "  text-align: center;",
      "  transition: all .18s ease;",
      "}",
      "#" + DROP_ID + ".is-dragover {",
      "  background: #e6f4ff;",
      "  border-color: #0958d9;",
      "  box-shadow: 0 0 0 3px rgba(22, 119, 255, .12);",
      "}",
      "#" + DROP_ID + " .course-drop-title {",
      "  color: #1677ff;",
      "  font-size: 15px;",
      "  font-weight: 600;",
      "}",
      "#" + DROP_ID + " .course-drop-tip {",
      "  color: #667085;",
      "  font-size: 12px;",
      "  margin-top: 6px;",
      "}",
      "#" + DROP_ID + " .course-drop-status {",
      "  color: #16a34a;",
      "  font-size: 12px;",
      "  margin-top: 8px;",
      "  min-height: 18px;",
      "}",
      "#" + DROP_ID + ".is-error .course-drop-status {",
      "  color: #ff4d4f;",
      "}",
    ].join("\n");
    document.head.appendChild(style);
  }

  function ensureAutoParseUpload() {
    if (window.__courseKnowledgeAutoParseUpload) {
      return;
    }
    window.__courseKnowledgeAutoParseUpload = true;

    var originalOpen = XMLHttpRequest.prototype.open;
    var originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;
    var originalSend = XMLHttpRequest.prototype.send;
    var capturedHeaders = {};
    var parsingIds = {};

    XMLHttpRequest.prototype.open = function (method, url) {
      this.__courseKnowledgeUploadMethod = String(method || "");
      this.__courseKnowledgeUploadUrl = String(url || "");
      return originalOpen.apply(this, arguments);
    };

    XMLHttpRequest.prototype.setRequestHeader = function (name, value) {
      var normalized = String(name || "").toLowerCase();
      if (
        normalized === "authorization" ||
        normalized === "clientid" ||
        normalized === "x-client-id" ||
        normalized === "tenant-id"
      ) {
        capturedHeaders[name] = value;
      }
      return originalSetRequestHeader.apply(this, arguments);
    };

    function buildParseUrl(listUrl, id) {
      var marker = "/system/attach/list";
      var source = String(listUrl || "");
      var index = source.indexOf(marker);
      if (index === -1) {
        return "/system/attach/parse/" + id;
      }
      return source.slice(0, index) + "/system/attach/parse/" + id;
    }

    function parseWaitingAttach(listUrl, row) {
      if (!row || !row.id || ["0", "3"].indexOf(String(row.status)) === -1 || parsingIds[row.id]) {
        return;
      }
      parsingIds[row.id] = true;
      notifyAutoParse("发现待解析附件：" + (row.name || row.id) + "，正在自动解析并更新索引...");
      var request = new XMLHttpRequest();
      request.open("POST", buildParseUrl(listUrl, row.id), true);
      Object.keys(capturedHeaders).forEach(function (key) {
        try {
          request.setRequestHeader(key, capturedHeaders[key]);
        } catch (error) {
          console.warn("Failed to reuse header for knowledge parse", key, error);
        }
      });
      request.addEventListener("load", function () {
        var payload = {};
        try {
          payload = JSON.parse(request.responseText || "{}");
        } catch (error) {
          payload = {};
        }
        if (request.status >= 200 && request.status < 300 && (payload.code === 200 || payload.code === undefined)) {
          console.info("知识库附件已触发自动解析", row.id);
          notifyAutoParse("已触发解析：" + (row.name || row.id) + "，稍后刷新列表即可查看知识片段。");
        } else {
          delete parsingIds[row.id];
          console.warn("知识库附件自动解析失败", row.id, request.status, request.responseText);
          notifyAutoParse("自动解析失败：" + (payload.msg || request.status || row.id) + "。请重新登录后台后再打开附件。", true);
        }
      });
      request.addEventListener("error", function () {
        delete parsingIds[row.id];
        notifyAutoParse("自动解析请求失败，请稍后刷新后台再试。", true);
      });
      request.send();
    }

    function inspectAttachListResponse(xhr) {
      if (!xhr.__courseKnowledgeUploadUrl || xhr.__courseKnowledgeUploadUrl.indexOf("/system/attach/list") === -1) {
        return;
      }
      try {
        var response = JSON.parse(xhr.responseText || "{}");
        var rows = Array.isArray(response.rows) ? response.rows : [];
        rows.forEach(function (row) {
          parseWaitingAttach(xhr.__courseKnowledgeUploadUrl, row);
        });
      } catch (error) {
        console.warn("Failed to inspect knowledge attachment list", error);
      }
    }

    XMLHttpRequest.prototype.send = function (body) {
      if (
        this.__courseKnowledgeUploadUrl &&
        this.__courseKnowledgeUploadUrl.indexOf("/system/attach/upload") !== -1 &&
        body instanceof FormData
      ) {
        if (!body.has("autoParse")) {
          body.append("autoParse", "true");
        }
      }
      if (this.__courseKnowledgeUploadUrl && this.__courseKnowledgeUploadUrl.indexOf("/system/attach/list") !== -1) {
        this.addEventListener("load", function () {
          inspectAttachListResponse(this);
        });
      }
      return originalSend.apply(this, arguments);
    };
  }

  function findAttachmentDrawer() {
    var drawers = Array.prototype.slice.call(document.querySelectorAll(".ant-drawer"));
    return drawers.find(function (drawer) {
      return drawer.textContent && drawer.textContent.indexOf("知识库附件") !== -1;
    });
  }

  function findUploadInput(drawer) {
    return drawer && drawer.querySelector('input[type="file"]');
  }

  function setStatus(dropZone, message, isError) {
    var status = dropZone.querySelector(".course-drop-status");
    if (status) {
      status.textContent = message || "";
    }
    dropZone.classList.toggle("is-error", Boolean(isError));
  }

  function notifyAutoParse(message, isError) {
    var dropZone = document.getElementById(DROP_ID);
    if (dropZone) {
      setStatus(dropZone, message, isError);
    }
  }

  function handFilesToOriginalUploader(drawer, dropZone, fileList) {
    var files = Array.prototype.slice.call(fileList || []).filter(Boolean);
    if (!files.length) {
      return;
    }

    var input = findUploadInput(drawer);
    if (!input) {
      setStatus(dropZone, "未找到原系统上传控件，请先点击“文件上传”按钮后再拖拽。", true);
      return;
    }

    try {
      var transfer = new DataTransfer();
      files.forEach(function (file) {
        transfer.items.add(file);
      });
      input.files = transfer.files;
      input.dispatchEvent(new Event("change", { bubbles: true }));
      setStatus(dropZone, "已接收 " + files.length + " 个文件，正在上传并更新索引...");
      window.setTimeout(function () {
        setStatus(dropZone, "上传任务已提交，可在列表中查看附件和知识片段。");
      }, 2500);
    } catch (error) {
      console.error(error);
      setStatus(dropZone, "浏览器未能接收拖拽文件，请改用“文件上传”按钮。", true);
    }
  }

  function bindDrawerDrop(drawer, dropZone) {
    if (!drawer || drawer.dataset.courseDragUploadBound === "1") {
      return;
    }
    drawer.dataset.courseDragUploadBound = "1";
    ["dragenter", "dragover"].forEach(function (eventName) {
      drawer.addEventListener(eventName, function (event) {
        if (event.dataTransfer && Array.prototype.indexOf.call(event.dataTransfer.types || [], "Files") !== -1) {
          event.preventDefault();
          dropZone.classList.add("is-dragover");
        }
      });
    });
    ["dragleave", "dragend"].forEach(function (eventName) {
      drawer.addEventListener(eventName, function () {
        dropZone.classList.remove("is-dragover");
      });
    });
    drawer.addEventListener("drop", function (event) {
      if (!event.dataTransfer || !event.dataTransfer.files || !event.dataTransfer.files.length) {
        return;
      }
      event.preventDefault();
      dropZone.classList.remove("is-dragover");
      handFilesToOriginalUploader(drawer, dropZone, event.dataTransfer.files);
    });
  }

  function ensureDropZone() {
    ensureStyle();
    var drawer = findAttachmentDrawer();
    if (!drawer || drawer.querySelector("#" + DROP_ID)) {
      return;
    }

    var uploadButton = Array.prototype.slice.call(drawer.querySelectorAll("button")).find(function (button) {
      return button.textContent && button.textContent.indexOf("文件上传") !== -1;
    });
    if (!uploadButton) {
      return;
    }

    var host = uploadButton.closest("div");
    if (!host || !host.parentNode) {
      return;
    }

    var dropZone = document.createElement("div");
    dropZone.id = DROP_ID;
    dropZone.innerHTML = [
      '<div class="course-drop-title">拖拽课程资料到这里即可上传</div>',
      '<div class="course-drop-tip">支持 PDF、Word、PPT、Excel、TXT、CSV、JSON 等资料；也可以点击这里选择文件。</div>',
      '<div class="course-drop-status"></div>',
    ].join("");

    dropZone.addEventListener("click", function () {
      var input = findUploadInput(drawer);
      if (input) {
        input.click();
      } else {
        uploadButton.click();
      }
    });
    ["dragenter", "dragover"].forEach(function (eventName) {
      dropZone.addEventListener(eventName, function (event) {
        event.preventDefault();
        event.stopPropagation();
        dropZone.classList.add("is-dragover");
      });
    });
    ["dragleave", "dragend", "drop"].forEach(function (eventName) {
      dropZone.addEventListener(eventName, function () {
        dropZone.classList.remove("is-dragover");
      });
    });
    dropZone.addEventListener("drop", function (event) {
      event.preventDefault();
      event.stopPropagation();
      handFilesToOriginalUploader(drawer, dropZone, event.dataTransfer && event.dataTransfer.files);
    });

    host.parentNode.insertBefore(dropZone, host.nextSibling);
    bindDrawerDrop(drawer, dropZone);
  }

  var observer = new MutationObserver(function () {
    ensureDropZone();
  });
  ensureAutoParseUpload();
  observer.observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener("DOMContentLoaded", ensureDropZone);
  window.setInterval(ensureDropZone, 1200);
})();
