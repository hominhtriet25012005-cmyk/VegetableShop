(() => {
    document.querySelectorAll('[data-print-receipt]').forEach(button => button.addEventListener('click', () => window.print()));
    const panel = document.querySelector('[data-payment-status-url]');
    if (!panel) return;
    const feedback = document.getElementById('payment-poll-feedback');
    let attempts = 0;
    async function poll() {
        if (attempts >= 120) {
            if (feedback) feedback.textContent = 'Đã dừng kiểm tra tự động sau 10 phút hoạt động. Tải lại trang để kiểm tra tiếp; không chuyển tiền lại.';
            return;
        }
        if (document.hidden) { window.setTimeout(poll, 5000); return; }
        attempts++;
        try {
            const response = await fetch(panel.dataset.paymentStatusUrl, {
                credentials: 'same-origin', cache: 'no-store', headers: {Accept: 'application/json'},
                signal: AbortSignal.timeout(8000)
            });
            if (response.redirected || response.status === 401 || response.status === 403) {
                if (feedback) feedback.textContent = 'Phiên đăng nhập đã thay đổi. Hãy tải lại trang và đăng nhập để xem thanh toán.';
                return;
            }
            if (!response.ok) throw new Error('status unavailable');
            const status = await response.json();
            if (typeof status.version !== 'string') throw new Error('invalid response');
            if (status.version !== panel.dataset.paymentVersion) { window.location.reload(); return; }
        } catch (_) {
            if (feedback) feedback.textContent = 'Chưa lấy được trạng thái mới, hệ thống sẽ thử lại. Không chuyển tiền lần nữa.';
        }
        window.setTimeout(poll, 5000);
    }
    window.setTimeout(poll, 5000);
})();
