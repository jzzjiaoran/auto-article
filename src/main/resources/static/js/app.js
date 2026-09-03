/*
 * auto-article 通用前端脚本
 * 提供：toast 提示、表单校验视觉反馈、确认删除对话框、文章编辑器工具栏等
 */
(function () {
  "use strict";

  /* ---------- Toast 提示 ---------- */
  function showToast(message, type) {
    type = type || "info";
    var container = document.getElementById("global-alert-container");
    if (!container) return;
    var icons = {
      success: "bi-check-circle",
      danger: "bi-exclamation-triangle",
      warning: "bi-exclamation-circle",
      info: "bi-info-circle"
    };
    var el = document.createElement("div");
    el.className = "toast align-items-center text-bg-" + (type === "danger" ? "danger" : type) + " border-0";
    el.setAttribute("role", "alert");
    el.setAttribute("aria-live", "assertive");
    el.setAttribute("aria-atomic", "true");
    el.innerHTML =
      '<div class="d-flex">' +
      '<div class="toast-body"><i class="bi ' + (icons[type] || icons.info) + ' me-2"></i>' +
      escapeHtml(message) + "</div>" +
      '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="关闭"></button>' +
      "</div>";
    container.appendChild(el);
    var toast = new bootstrap.Toast(el, { delay: 3500 });
    toast.show();
    el.addEventListener("hidden.bs.toast", function () {
      container.removeChild(el);
    });
  }

  function escapeHtml(str) {
    var div = document.createElement("div");
    div.textContent = str == null ? "" : String(str);
    return div.innerHTML;
  }

  /* 从请求参数读取前端提示（当前页刚完成操作时由 Controller 注入） */
  function readFlashFromBody() {
    var success = document.body.getAttribute("data-flash-success");
    var error = document.body.getAttribute("data-flash-error");
    if (success) showToast(success, "success");
    if (error) showToast(error, "danger");
  }

  /* ---------- 表单校验视觉反馈 ---------- */
  function enableValidationFeedback() {
    var forms = document.querySelectorAll("form.needs-validation");
    Array.prototype.forEach.call(forms, function (form) {
      form.addEventListener("submit", function (event) {
        if (!form.checkValidity()) {
          event.preventDefault();
          event.stopPropagation();
        }
        form.classList.add("was-validated");
      }, false);
    });
  }

  /* ---------- 确认删除 ---------- */
  function enableDangerButtons() {
    var buttons = document.querySelectorAll('[data-confirm]');
    Array.prototype.forEach.call(buttons, function (btn) {
      btn.addEventListener("click", function (e) {
        var message = btn.getAttribute("data-confirm") ||
          "确定要执行此操作吗？此操作不可恢复。";
        if (!window.confirm(message)) {
          e.preventDefault();
          e.stopPropagation();
        }
      });
    });
  }

  /* ---------- 文章编辑器工具栏（增强普通 textarea 为简易 Markdown 编辑器） ---------- */
  function initArticleEditor() {
    var textarea = document.getElementById("articleContent");
    if (!textarea) return;
    var wrap = textarea.closest(".article-editor-wrap");
    if (!wrap) return;

    function wrapSelection(symStart, symEnd, placeholder) {
      var start = textarea.selectionStart;
      var end = textarea.selectionEnd;
      var selected = textarea.value.substring(start, end) || placeholder || "";
      var replacement = symStart + selected + symEnd;
      textarea.setRangeText(replacement, start, end, "end");
      textarea.dispatchEvent(new Event("input"));
      updateWordCount();
      textarea.focus();
    }

    function updateWordCount() {
      var counter = document.getElementById("articleWordCount");
      if (counter) {
        counter.textContent = textarea.value.trim().length;
      }
    }

    var toolbar = wrap.querySelector(".editor-toolbar");
    if (toolbar) {
      toolbar.addEventListener("click", function (e) {
        var btn = e.target.closest("button[data-action]");
        if (!btn) return;
        var action = btn.getAttribute("data-action");
        switch (action) {
          case "h2": wrapSelection("## ", "", "二级标题"); break;
          case "h3": wrapSelection("### ", "", "三级标题"); break;
          case "bold": wrapSelection("**", "**", "加粗文字"); break;
          case "italic": wrapSelection("*", "*", "斜体文字"); break;
          case "link": wrapSelection("[", "](https://)", "链接文字"); break;
          case "list": wrapSelection("\n- ", "", "列表项"); break;
          case "quote": wrapSelection("> ", "", "引用内容"); break;
          case "code": wrapSelection("```\n", "\n```", "代码"); break;
        }
      });
    }

    textarea.addEventListener("input", updateWordCount);
    updateWordCount();
  }

  /* ---------- 带进度的 AI 生成轮询 ---------- */
  function initGeneration() {
    var box = document.getElementById("generation-box");
    if (!box) return;
    var taskId = box.getAttribute("data-task-id");
    var pollUrl = box.getAttribute("data-poll-url");
    if (!taskId || !pollUrl) return;

    var bar = document.getElementById("generation-bar");
    var statusText = document.getElementById("generation-status");
    var resultLink = document.getElementById("generation-result-link");

    var timer = setInterval(function () {
      fetch(pollUrl, { headers: { "Accept": "application/json" } })
        .then(function (resp) { return resp.json(); })
        .then(function (data) {
          var status = data.status || "unknown";
          if (bar) {
            var pct = status === "success" ? 100 : (status === "failed" ? 100 : 50);
            bar.style.width = pct + "%";
          }
          if (statusText) {
            statusText.textContent = data.message || status;
          }
          if (status === "success" && resultLink && data.articleId) {
            resultLink.href = "/articles/" + data.articleId;
            resultLink.classList.remove("d-none");
            clearInterval(timer);
          } else if (status === "failed") {
            clearInterval(timer);
          }
        })
        .catch(function () { /* 忽略轮询中断 */ });
    }, 2000);
  }

  /* 根据选中热点填充标题 */
  function initTopicLink() {
    var topicSelect = document.getElementById("topicSelect");
    var titleInput = document.getElementById("articleTitle");
    if (!topicSelect || !titleInput) return;
    topicSelect.addEventListener("change", function () {
      var opt = topicSelect.selectedOptions[0];
      if (opt && opt.getAttribute("data-title")) {
        titleInput.value = opt.getAttribute("data-title");
      }
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    readFlashFromBody();
    enableValidationFeedback();
    enableDangerButtons();
    initArticleEditor();
    initGeneration();
    initTopicLink();
  });

  // 暴露到全局，供内联脚本使用
  window.aaToast = showToast;
})();
