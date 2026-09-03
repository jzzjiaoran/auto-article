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

  /* ---------- AI 生成：JSON 提交与任务轮询 ---------- */

  /* 解析后端统一返回体 Result：成功 code 为 0 或 200，实际数据在 data 字段 */
  function isResultOk(body) {
    return body && (body.code === 0 || body.code === 200) && body.data != null;
  }

  function extractErrorMessage(body, fallback) {
    return (body && body.message) || fallback || "操作失败，请稍后重试";
  }

  /* 生成进度面板状态更新 */
  function setGenerationState(text, state) {
    var statusText = document.getElementById("generation-status");
    var bar = document.getElementById("generation-bar");
    var hint = document.getElementById("generation-hint");
    if (statusText) statusText.textContent = text;
    if (hint) hint.classList.add("d-none");
    if (bar && bar.classList.contains("d-none")) bar.classList.remove("d-none");
    if (!bar) return;
    bar.classList.remove("bg-success", "bg-danger");
    bar.classList.add("progress-bar-striped", "progress-bar-animated");
    bar.style.width = "50%";
    if (state === "success") {
      bar.classList.remove("progress-bar-animated");
      bar.classList.add("bg-success");
      bar.style.width = "100%";
    } else if (state === "failed") {
      bar.classList.remove("progress-bar-animated");
      bar.classList.add("bg-danger");
      bar.style.width = "100%";
    }
  }

  function showGenerationResultLink(articleId) {
    var link = document.getElementById("generation-result-link");
    if (!link || !articleId) return;
    link.href = "/articles/" + articleId;
    link.classList.remove("d-none");
  }

  function setButtonLoading(btn) {
    if (!btn) return;
    btn.dataset.original = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>处理中...';
  }

  function restoreButton(btn) {
    if (!btn) return;
    btn.disabled = false;
    if (btn.dataset.original) btn.innerHTML = btn.dataset.original;
  }

  /* 轮询单个生成任务并刷新进度面板；stopWhenDone 为 true 时到达终态即停止 */
  function pollGenerationTask(taskId, stopWhenDone, onDone) {
    if (!taskId) return;
    var statusText = document.getElementById("generation-status");
    var taskLabel = document.getElementById("generation-task-label");
    if (taskLabel) taskLabel.textContent = "任务#" + taskId;
    var timer = setInterval(function () {
      fetch("/tasks/" + encodeURIComponent(taskId), { headers: { "Accept": "application/json" } })
        .then(function (resp) { return resp.json(); })
        .then(function (body) {
          if (!isResultOk(body)) {
            if (onDone) onDone(false, extractErrorMessage(body, "查询任务状态失败"));
            clearInterval(timer);
            return;
          }
          var task = body.data || {};
          var status = task.status || "unknown";
          var message = task.message || status;
          if (statusText && status !== "pending") statusText.textContent = message;
          if (status === "completed" || status === "success") {
            setGenerationState(message, "success");
            showGenerationResultLink(task.articleId);
            clearInterval(timer);
            if (onDone) onDone(true, message);
          } else if (status === "failed") {
            setGenerationState(message, "failed");
            clearInterval(timer);
            if (onDone) onDone(false, message);
          } else if (!stopWhenDone) {
            setGenerationState(message, "running");
          }
        })
        .catch(function () { /* 忽略轮询中断，下轮继续 */ });
    }, 2000);
  }

  /* 生成页表单：拦截提交并改为 fetch JSON POST /articles */
  function initGenerateForm() {
    var form = document.getElementById("generateForm");
    if (!form) return;
    var submitBtn = document.getElementById("generateSubmitBtn");

    form.addEventListener("submit", function (event) {
      if (!form.checkValidity()) {
        form.classList.add("was-validated");
        return;
      }
      event.preventDefault();
      event.stopPropagation();

      setButtonLoading(submitBtn);

      var topicSelect = document.getElementById("topicSelect");
      var titleInput = document.getElementById("articleTitle");
      var title = titleInput ? titleInput.value.trim() : "";
      if (!title && topicSelect && topicSelect.selectedOptions && topicSelect.selectedOptions[0]) {
        var opt = topicSelect.selectedOptions[0];
        var autoTitle = opt.getAttribute("data-title");
        if (autoTitle) title = autoTitle;
      }

      var payload = {
        topicId: topicSelect && topicSelect.value ? Number(topicSelect.value) : null,
        title: title,
        style: (document.getElementById("style") || {}).value || "popular",
        length: (document.getElementById("length") || {}).value || "medium",
        prompt: (document.getElementById("prompt") || {}).value || "",
        saveDraft: !!(document.getElementById("saveDraft") || {}).checked
      };

      fetch(form.action, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Accept": "application/json" },
        body: JSON.stringify(payload)
      })
        .then(function (resp) { return resp.json().catch(function () { return null; }); })
        .then(function (body) {
          if (!isResultOk(body)) {
            showToast(extractErrorMessage(body, "提交失败，请稍后重试"), "danger");
            restoreButton(submitBtn);
            return;
          }
          var taskId = body.data;
          setGenerationState("任务已提交，正在排队...", "running");
          pollGenerationTask(taskId, false, function () {
            restoreButton(submitBtn);
          });
        })
        .catch(function () {
          showToast("网络异常，提交失败，请稍后重试", "danger");
          restoreButton(submitBtn);
        });
    });
  }

  /* 文章详情页「重新生成」：改为 fetch 提交，避免浏览器跳转显示裸 JSON */
  function initRegenerateForm() {
    var form = document.getElementById("regenerateForm");
    if (!form) return;
    var submitBtn = form.querySelector("button[type=submit]");

    form.addEventListener("submit", function (event) {
      event.preventDefault();
      event.stopPropagation();

      setButtonLoading(submitBtn);

      fetch(form.action, {
        method: "POST",
        headers: { "Accept": "application/json" }
      })
        .then(function (resp) { return resp.json().catch(function () { return null; }); })
        .then(function (body) {
          if (!isResultOk(body)) {
            showToast(extractErrorMessage(body, "重新生成提交失败"), "danger");
            restoreButton(submitBtn);
            return;
          }
          var taskId = body.data;
          showToast("重新生成任务已提交，完成将自动刷新本页", "success");
          pollGenerationTask(taskId, true, function (ok, message) {
            if (ok) {
              window.location.reload();
            } else {
              showToast(message || "重新生成失败", "danger");
              restoreButton(submitBtn);
            }
          });
        })
        .catch(function () {
          showToast("网络异常，重新生成提交失败", "danger");
          restoreButton(submitBtn);
        });
    });
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
    initGenerateForm();
    initRegenerateForm();
    initTopicLink();
  });

  // 暴露到全局，供内联脚本使用
  window.aaToast = showToast;
})();
