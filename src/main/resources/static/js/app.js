// 统一的确认弹窗：模板里用 data-confirm 声明提示语，避免内联 onclick（CSP 禁止内联脚本）。
document.addEventListener('submit', function (event) {
    const form = event.target;
    if (!(form instanceof HTMLFormElement)) {
        return;
    }
    const message = form.getAttribute('data-confirm');
    if (message && !window.confirm(message)) {
        event.preventDefault();
    }
});
