"use strict";

(function () {
  function initProductEditor() {
    var editor = document.querySelector("[data-rich-text-editor]");

    if (!editor || typeof window.jQuery === "undefined" || !window.jQuery.fn.summernote) {
      return;
    }

    window.jQuery(editor).summernote({
      height: 260,
      placeholder: "Nhập mô tả chi tiết của sản phẩm...",
      disableDragAndDrop: true,
      toolbar: [
        ["style", ["style"]],
        ["font", ["bold", "italic", "underline", "clear"]],
        ["para", ["ul", "ol", "paragraph"]],
        ["insert", ["link"]],
        ["view", ["undo", "redo", "codeview"]]
      ]
    });

    editor.form.addEventListener("submit", function () {
      editor.value = window.jQuery(editor).summernote("code");
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initProductEditor);
  } else {
    initProductEditor();
  }
})();
