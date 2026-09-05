(() => {
    const feedback = document.getElementById('copy-feedback');
    document.querySelectorAll('[data-copy]').forEach(button => button.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(button.dataset.copy);
            if (feedback) feedback.textContent = 'Đã sao chép.';
        } catch (_) {
            if (feedback) feedback.textContent = 'Trình duyệt không cho sao chép. Bạn hãy chọn và sao chép thông tin hiển thị.';
        }
    }));
})();
